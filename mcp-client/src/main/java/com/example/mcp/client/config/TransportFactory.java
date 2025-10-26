package com.example.mcp.client.config;

import com.example.mcp.client.transport.GrpcTransport;
import com.example.mcp.client.transport.HttpTransport;
import com.example.mcp.client.transport.SdkTransport;
import com.example.mcp.client.transport.Transport;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Interceptor;

public class TransportFactory {
  public Transport create(String serverName, ServerConfig serverConfig) {
    if (serverConfig == null) {
      throw new IllegalArgumentException("No server configuration found for " + serverName);
    }
    switch (serverConfig.getType()) {
      case HTTP:
        return createHttpTransport(serverConfig);
      case GRPC:
        return createGrpcTransport(serverConfig);
      case SDK:
        return createSdkTransport(serverConfig);
      default:
        throw new IllegalArgumentException("Unsupported transport type " + serverConfig.getType());
    }
  }

  private Transport createHttpTransport(ServerConfig serverConfig) {
    if (serverConfig.getBaseUrl() == null) {
      throw new IllegalArgumentException("HTTP transport requires baseUrl");
    }
    List<Interceptor> interceptors = new ArrayList<>();
    for (InterceptorConfig interceptorConfig : serverConfig.getInterceptors()) {
      interceptors.add(instantiateInterceptor(interceptorConfig));
    }
    return new HttpTransport(serverConfig.getBaseUrl(), interceptors);
  }

  private Transport createGrpcTransport(ServerConfig serverConfig) {
    if (serverConfig.getHost() == null || serverConfig.getPort() == null) {
      throw new IllegalArgumentException("gRPC transport requires host and port");
    }
    return new GrpcTransport(serverConfig.getHost(), serverConfig.getPort());
  }

  private Transport createSdkTransport(ServerConfig serverConfig) {
    if (serverConfig.getSdkClass() == null) {
      return SdkTransport.autoDetect();
    }
    try {
      Class<?> sdkClass = Class.forName(serverConfig.getSdkClass());
      Object instance = sdkClass.getDeclaredConstructor().newInstance();
      return new SdkTransport(instance);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Failed to instantiate SDK transport for " + serverConfig.getSdkClass(), ex);
    }
  }

  private Interceptor instantiateInterceptor(InterceptorConfig config) {
    if (config.getClassName() == null) {
      throw new IllegalArgumentException("Interceptor className is required");
    }
    try {
      Class<?> clazz = Class.forName(config.getClassName());
      if (!Interceptor.class.isAssignableFrom(clazz)) {
        throw new IllegalArgumentException(config.getClassName() + " is not an okhttp3.Interceptor");
      }
      List<String> args = config.getArgs();
      if (args == null || args.isEmpty()) {
        return (Interceptor) clazz.getDeclaredConstructor().newInstance();
      }
      Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors) {
        if (constructor.getParameterCount() == args.size()) {
          Object[] resolvedArgs = new Object[args.size()];
          Class<?>[] parameterTypes = constructor.getParameterTypes();
          boolean compatible = true;
          for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramType = parameterTypes[i];
            if (paramType.equals(String.class)) {
              resolvedArgs[i] = args.get(i);
            } else if (paramType.equals(int.class) || paramType.equals(Integer.class)) {
              resolvedArgs[i] = Integer.parseInt(args.get(i));
            } else if (paramType.equals(long.class) || paramType.equals(Long.class)) {
              resolvedArgs[i] = Long.parseLong(args.get(i));
            } else if (paramType.equals(boolean.class) || paramType.equals(Boolean.class)) {
              resolvedArgs[i] = Boolean.parseBoolean(args.get(i));
            } else {
              compatible = false;
              break;
            }
          }
          if (compatible) {
            constructor.setAccessible(true);
            return (Interceptor) constructor.newInstance(resolvedArgs);
          }
        }
      }
      throw new IllegalArgumentException(
          "No compatible constructor found for interceptor " + config.getClassName());
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Failed to instantiate interceptor " + config.getClassName(), ex);
    }
  }
}
