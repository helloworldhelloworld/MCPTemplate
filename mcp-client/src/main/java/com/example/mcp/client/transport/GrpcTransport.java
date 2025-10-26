package com.example.mcp.client.transport;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Iterator;
import java.util.function.Consumer;

public class GrpcTransport implements Transport {
  private final ManagedChannel channel;
  private final MethodDescriptor<String, String> invokeMethod;
  private final MethodDescriptor<String, String> streamMethod;

  public GrpcTransport(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
  }

  public GrpcTransport(ManagedChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
    this.invokeMethod = buildUnaryMethod("mcp.McpService", "Invoke");
    this.streamMethod = buildServerStreamMethod("mcp.McpService", "Stream");
  }

  @Override
  public String postJson(String path, String json) {
    return ClientCalls.blockingUnaryCall(
        channel, invokeMethod, CallOptions.DEFAULT, json == null ? "" : json);
  }

  @Override
  public void getSse(String path, Consumer<String> onEvent) {
    Iterator<String> responses =
        ClientCalls.blockingServerStreamingCall(
            channel, streamMethod, CallOptions.DEFAULT, path);
    while (responses.hasNext()) {
      String item = responses.next();
      onEvent.accept(item);
    }
  }

  public void shutdown() {
    channel.shutdownNow();
  }

  private MethodDescriptor<String, String> buildUnaryMethod(String service, String method) {
    return MethodDescriptor.<String, String>newBuilder()
        .setFullMethodName(MethodDescriptor.generateFullMethodName(service, method))
        .setType(MethodDescriptor.MethodType.UNARY)
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();
  }

  private MethodDescriptor<String, String> buildServerStreamMethod(String service, String method) {
    return MethodDescriptor.<String, String>newBuilder()
        .setFullMethodName(MethodDescriptor.generateFullMethodName(service, method))
        .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();
  }

  private static class StringMarshaller implements MethodDescriptor.Marshaller<String> {
    @Override
    public InputStream stream(String value) {
      return new ByteArrayInputStream((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String parse(InputStream stream) {
      try {
        byte[] buffer = new byte[1024];
        int read;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        while ((read = stream.read(buffer)) != -1) {
          out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
