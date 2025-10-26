package com.example.mcp.client.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.mcp.client.config.McpClientConfig;
import com.example.mcp.client.config.RouteConfig;
import com.example.mcp.client.config.ServerConfig;
import com.example.mcp.client.config.TransportType;
import com.example.mcp.common.StdResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class McpRouteClientTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void invokeBlockingReturnsStdResponse() throws Exception {
    McpClientConfig config = buildConfig();
    try (McpClientEnvironment environment = new McpClientEnvironment(config)) {
      environment.start();
      McpRouteClient routeClient = new McpRouteClient(environment);

      StdResponse<?> response =
          routeClient.invokeBlocking("vehicleState", Map.of("vehicleId", "veh-42"));

      assertEquals("success", response.getStatus());
      JsonNode node = MAPPER.valueToTree(response.getData());
      assertEquals("veh-42", node.path("vehicleId").asText());
      assertEquals(87.5, node.path("batteryPercentage").asDouble());
    }
  }

  @Test
  void concurrentInvocationsMaintainCorrelation() throws Exception {
    McpClientConfig config = buildConfig();
    try (McpClientEnvironment environment = new McpClientEnvironment(config)) {
      environment.start();
      McpRouteClient routeClient = new McpRouteClient(environment);

      CompletableFuture<StdResponse<?>> first =
          routeClient.invoke("vehicleState", Map.of("vehicleId", "veh-A")).toCompletableFuture();
      CompletableFuture<StdResponse<?>> second =
          routeClient.invoke("vehicleState", Map.of("vehicleId", "veh-B")).toCompletableFuture();

      JsonNode firstNode = MAPPER.valueToTree(first.get().getData());
      JsonNode secondNode = MAPPER.valueToTree(second.get().getData());

      assertEquals("veh-A", firstNode.path("vehicleId").asText());
      assertEquals("veh-B", secondNode.path("vehicleId").asText());
    }
  }

  private McpClientConfig buildConfig() {
    McpClientConfig config = new McpClientConfig();
    config.setClientId("test-client");
    config.setDefaultServer("sdk");

    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setType(TransportType.SDK);
    serverConfig.setSdkClass(StubSdkClient.class.getName());
    config.setServers(Map.of("sdk", serverConfig));

    RouteConfig routeConfig = new RouteConfig();
    routeConfig.setTool("mcp.vehicle.state.get");
    config.setRoutes(Map.of("vehicleState", routeConfig));
    return config;
  }

  public static class StubSdkClient {
    private final ObjectMapper mapper = new ObjectMapper();

    public String postJson(String path, String json) throws Exception {
      JsonNode root = mapper.readTree(json);
      String vehicleId = root.path("payload").path("vehicleId").asText();
      return "{\"status\":\"success\",\"code\":\"OK\",\"message\":\"done\",\"data\":"
          + "{\"vehicleId\":\""
          + vehicleId
          + "\",\"batteryPercentage\":87.5}}";
    }

    public void getSse(String path, java.util.function.Consumer<String> consumer) {
      // no-op for tests
    }
  }
}
