package com.example.mcp.client.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.mcp.client.McpClient;
import com.example.mcp.client.config.McpClientConfig;
import com.example.mcp.client.config.RouteConfig;
import com.example.mcp.client.config.ServerConfig;
import com.example.mcp.client.runtime.McpClientRegistry;
import com.example.mcp.common.StdResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class McpRouteClientTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void invokeBlockingReturnsStdResponse() throws Exception {
    McpClientConfig config = buildConfig();
    McpClient stubClient = Mockito.mock(McpClient.class);
    when(stubClient.invokeAsync(any(), any(), any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                StdResponse.success(
                    "OK",
                    "done",
                    Map.of("vehicleId", "veh-42", "batteryPercentage", 87.5))));

    try (McpClientEnvironment environment =
        new McpClientEnvironment(config, new McpClientRegistry(config, Map.of("stub", stubClient)))) {
      environment.start();
      McpRouteClient routeClient = new McpRouteClient(environment);

      StdResponse<?> response =
          routeClient.invokeBlocking("vehicleState", Map.of("vehicleId", "veh-42"));

      assertEquals("success", response.getStatus());
      assertEquals("veh-42", MAPPER.valueToTree(response.getData()).path("vehicleId").asText());
    }
  }

  @Test
  void concurrentInvocationsMaintainCorrelation() throws Exception {
    McpClientConfig config = buildConfig();
    McpClient stubClient = Mockito.mock(McpClient.class);
    when(stubClient.invokeAsync(any(), any(), any()))
        .thenAnswer(
            invocation ->
                CompletableFuture.completedFuture(
                    StdResponse.success(
                        "OK",
                        "done",
                        Map.of(
                            "vehicleId",
                            ((Map<?, ?>) invocation.getArgument(1)).get("vehicleId"))));

    try (McpClientEnvironment environment =
        new McpClientEnvironment(config, new McpClientRegistry(config, Map.of("stub", stubClient)))) {
      environment.start();
      McpRouteClient routeClient = new McpRouteClient(environment);

      CompletionStage<StdResponse<?>> first =
          routeClient.invoke("vehicleState", Map.of("vehicleId", "veh-A"));
      CompletionStage<StdResponse<?>> second =
          routeClient.invoke("vehicleState", Map.of("vehicleId", "veh-B"));

      assertEquals(
          "veh-A", MAPPER.valueToTree(first.toCompletableFuture().get().getData()).path("vehicleId").asText());
      assertEquals(
          "veh-B", MAPPER.valueToTree(second.toCompletableFuture().get().getData()).path("vehicleId").asText());
    }
  }

  private McpClientConfig buildConfig() {
    McpClientConfig config = new McpClientConfig();
    config.setClientId("test-client");
    config.setDefaultServer("stub");
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setBaseUrl("http://localhost");
    config.setServers(Map.of("stub", serverConfig));
    RouteConfig routeConfig = new RouteConfig();
    routeConfig.setTool("mcp.vehicle.state.get");
    config.setRoutes(Map.of("vehicleState", routeConfig));
    return config;
  }
}
