package com.example.mcp.client.springai;

import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.StreamEventEnvelope;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.stream.Stream;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Thin reflective facade over the Spring AI MCP client so the rest of the application does not
 * bind to the framework directly. The bridge searches for the expected builder and operation
 * methods on the classpath at runtime, allowing alternative MCP implementations to be provided in
 * the future by replacing this component.
 */
public class SpringAiMcpClientBridge implements AutoCloseable {

  private static final String SPRING_AI_CLIENT_CLASS = "org.springframework.ai.mcp.client.McpClient";
  private static final String SPRING_AI_HTTP_TRANSPORT_CLASS =
      "org.springframework.ai.mcp.client.transport.http.HttpMcpClientTransport";

  private final Object delegate;
  private final Method openSessionMethod;
  private final Method discoveryMethod;
  private final Method invokeMethod;
  private final Method notificationsMethod;
  private final Method governanceMethod;

  public SpringAiMcpClientBridge(String clientId, String baseUrl) {
    try {
      this.delegate = instantiateClient(clientId, baseUrl);
      Class<?> clientType = delegate.getClass();
      this.openSessionMethod = resolveSingleArgMethod(clientType, "openSession", SessionOpenRequest.class);
      this.discoveryMethod = resolveNoArgMethod(clientType, "listTools");
      this.invokeMethod = resolveSingleArgMethod(clientType, "invoke", RequestEnvelope.class);
      this.notificationsMethod = resolveNoArgMethod(clientType, "notifications");
      this.governanceMethod = resolveSingleArgMethod(clientType, "governance", String.class);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize Spring AI MCP client", ex);
    }
  }

  @SuppressWarnings("unchecked")
  public StdResponse<SessionOpenResponse> openSession(SessionOpenRequest request) {
    Object result = invokeMono(openSessionMethod, request);
    return (StdResponse<SessionOpenResponse>) result;
  }

  @SuppressWarnings("unchecked")
  public StdResponse<List<ToolDescriptor>> discoverTools() {
    Object result = invokeMono(discoveryMethod);
    return (StdResponse<List<ToolDescriptor>>) result;
  }

  @SuppressWarnings("unchecked")
  public StdResponse<JsonNode> invoke(RequestEnvelope requestEnvelope) {
    Object result = invokeMono(invokeMethod, requestEnvelope);
    return (StdResponse<JsonNode>) result;
  }

  public Flux<StreamEventEnvelope<JsonNode>> notifications(Duration timeout) {
    Object publisher = invokeRaw(notificationsMethod);
    Flux<?> flux = adaptFlux(publisher);
    if (timeout != null) {
      flux = flux.timeout(timeout);
    }
    @SuppressWarnings("unchecked")
    Flux<StreamEventEnvelope<JsonNode>> typed = (Flux<StreamEventEnvelope<JsonNode>>) flux;
    return typed;
  }

  @SuppressWarnings("unchecked")
  public StdResponse<GovernanceReport> governance(String requestId) {
    Object result = invokeMono(governanceMethod, requestId);
    return (StdResponse<GovernanceReport>) result;
  }

  @Override
  public void close() {
    try {
      Method close = delegate.getClass().getMethod("close");
      close.invoke(delegate);
    } catch (NoSuchMethodException ignored) {
    } catch (IllegalAccessException | InvocationTargetException ex) {
      throw new IllegalStateException("Failed to close Spring AI MCP client", ex);
    }
  }

  private Object invokeMono(Method method, Object... args) {
    Object publisher = invokeRaw(method, args);
    Mono<?> mono = adaptMono(publisher);
    return mono.blockOptional().orElse(null);
  }

  private Object invokeRaw(Method method, Object... args) {
    try {
      return method.invoke(delegate, args);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      throw new IllegalStateException("Spring AI MCP invocation failed", ex.getCause());
    }
  }

  private Mono<?> adaptMono(Object publisher) {
    if (publisher instanceof Mono<?> mono) {
      return mono;
    }
    if (publisher instanceof Publisher<?> reactivePublisher) {
      return Mono.from(reactivePublisher);
    }
    if (publisher instanceof Optional<?> optional) {
      return Mono.justOrEmpty(optional);
    }
    return Mono.justOrEmpty(publisher);
  }

  private Flux<?> adaptFlux(Object publisher) {
    if (publisher instanceof Flux<?> flux) {
      return flux;
    }
    if (publisher instanceof Publisher<?> reactivePublisher) {
      return Flux.from(reactivePublisher);
    }
    if (publisher instanceof Iterable<?> iterable) {
      return Flux.fromIterable(iterable);
    }
    if (publisher instanceof Stream<?> stream) {
      return Flux.fromStream(stream);
    }
    return Flux.empty();
  }

  private Object instantiateClient(String clientId, String baseUrl) throws Exception {
    Class<?> clientClass = Class.forName(SPRING_AI_CLIENT_CLASS);
    Object transport = instantiateHttpTransport(baseUrl);
    Optional<Method> builderMethod = Stream.of(clientClass.getMethods())
        .filter(method -> method.getName().equals("builder") && method.getParameterCount() == 0)
        .findFirst();
    if (builderMethod.isPresent()) {
      Object builder = builderMethod.get().invoke(null);
      configureBuilder(builder, clientId, transport);
      Method build = builder.getClass().getMethod("build");
      return build.invoke(builder);
    }
    for (Constructor<?> constructor : clientClass.getDeclaredConstructors()) {
      Object candidate = tryInstantiate(constructor, clientId, transport);
      if (candidate != null) {
        return candidate;
      }
    }
    throw new IllegalStateException("Unable to construct Spring AI MCP client");
  }

  private void configureBuilder(Object builder, String clientId, Object transport) throws Exception {
    Class<?> builderType = builder.getClass();
    boolean clientSet = false;
    boolean transportSet = false;
    for (Method method : builderType.getMethods()) {
      if (method.getParameterCount() != 1) {
        continue;
      }
      Class<?> paramType = method.getParameterTypes()[0];
      String methodName = method.getName().toLowerCase(Locale.ROOT);
      if (!clientSet && paramType.equals(String.class) && methodName.contains("client")) {
        method.invoke(builder, clientId);
        clientSet = true;
      } else if (!transportSet && paramType.isInstance(transport)) {
        method.invoke(builder, transport);
        transportSet = true;
      }
    }
    if (!clientSet) {
      // Fall back to the first String setter if a dedicated clientId method is not present.
      for (Method method : builderType.getMethods()) {
        if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(String.class)) {
          method.invoke(builder, clientId);
          clientSet = true;
          break;
        }
      }
    }
    if (!transportSet) {
      for (Method method : builderType.getMethods()) {
        if (method.getParameterCount() == 1
            && method.getParameterTypes()[0].isAssignableFrom(transport.getClass())) {
          method.invoke(builder, transport);
          transportSet = true;
          break;
        }
      }
    }
    if (!clientSet) {
      throw new IllegalStateException("Spring AI MCP client builder does not expose client id setter");
    }
    if (!transportSet) {
      throw new IllegalStateException("Spring AI MCP client builder does not expose transport setter");
    }
  }

  private Object tryInstantiate(Constructor<?> constructor, String clientId, Object transport)
      throws Exception {
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    Object[] args = new Object[parameterTypes.length];
    boolean clientSet = false;
    boolean transportSet = false;
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> type = parameterTypes[i];
      if (!clientSet && type.equals(String.class)) {
        args[i] = clientId;
        clientSet = true;
      } else if (!transportSet && type.isInstance(transport)) {
        args[i] = transport;
        transportSet = true;
      } else {
        return null;
      }
    }
    constructor.setAccessible(true);
    return constructor.newInstance(args);
  }

  private Object instantiateHttpTransport(String baseUrl) throws Exception {
    Class<?> transportClass = Class.forName(SPRING_AI_HTTP_TRANSPORT_CLASS);
    for (Method method : transportClass.getMethods()) {
      if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(String.class)) {
        Object transport = method.invoke(null, baseUrl);
        if (transport != null) {
          return transport;
        }
      }
    }
    for (Constructor<?> constructor : transportClass.getDeclaredConstructors()) {
      if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].equals(String.class)) {
        constructor.setAccessible(true);
        return constructor.newInstance(baseUrl);
      }
    }
    throw new IllegalStateException("Unable to create Spring AI HTTP MCP transport");
  }

  private Method resolveNoArgMethod(Class<?> type, String name) throws NoSuchMethodException {
    try {
      return type.getMethod(name);
    } catch (NoSuchMethodException ex) {
      for (Method method : type.getMethods()) {
        if (method.getParameterCount() == 0
            && Publisher.class.isAssignableFrom(method.getReturnType())) {
          return method;
        }
      }
      throw ex;
    }
  }

  private Method resolveSingleArgMethod(Class<?> type, String name, Class<?> parameterType)
      throws NoSuchMethodException {
    try {
      return type.getMethod(name, parameterType);
    } catch (NoSuchMethodException ex) {
      for (Method method : type.getMethods()) {
        if (method.getParameterCount() == 1
            && method.getParameterTypes()[0].isAssignableFrom(parameterType)) {
          return method;
        }
      }
      throw ex;
    }
  }
}
