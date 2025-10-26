package com.example.mcp.client.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class GrpcTransportTest {

  @Test
  void postJsonDelegatesToClientCalls() throws Exception {
    ManagedChannel channel = Mockito.mock(ManagedChannel.class);
    GrpcTransport transport = new GrpcTransport(channel);

    try (MockedStatic<ClientCalls> clientCalls = Mockito.mockStatic(ClientCalls.class)) {
      clientCalls
          .when(() -> ClientCalls.blockingUnaryCall(Mockito.eq(channel), Mockito.any(), Mockito.eq(CallOptions.DEFAULT), Mockito.eq("payload")))
          .thenReturn("response");

      String result = transport.postJson("/mcp/invoke", "payload");

      assertEquals("response", result);
    }
  }

  @Test
  void getSseStreamsAllItems() throws Exception {
    ManagedChannel channel = Mockito.mock(ManagedChannel.class);
    GrpcTransport transport = new GrpcTransport(channel);

    try (MockedStatic<ClientCalls> clientCalls = Mockito.mockStatic(ClientCalls.class)) {
      Iterator<String> iterator = Arrays.asList("one", "two").iterator();
      clientCalls
          .when(() -> ClientCalls.blockingServerStreamingCall(Mockito.eq(channel), Mockito.any(), Mockito.eq(CallOptions.DEFAULT), Mockito.eq("/mcp/stream")))
          .thenReturn(iterator);
      AtomicBoolean accepted = new AtomicBoolean();

      transport.getSse("/mcp/stream", value -> {
        if ("two".equals(value)) {
          accepted.set(true);
        }
      });

      assertTrue(accepted.get());
    }
  }

  @Test
  void shutdownCallsChannel() {
    ManagedChannel channel = Mockito.mock(ManagedChannel.class);
    GrpcTransport transport = new GrpcTransport(channel);

    transport.shutdown();

    Mockito.verify(channel).shutdownNow();
  }
}
