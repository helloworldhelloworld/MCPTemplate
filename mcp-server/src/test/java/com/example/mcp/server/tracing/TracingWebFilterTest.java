package com.example.mcp.server.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class TracingWebFilterTest {

  private final TracingWebFilter filter = new TracingWebFilter();

  @Test
  void nonMcpPathsAreBypassed() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/health"));
    TrackingChain chain = new TrackingChain();

    filter.filter(exchange, chain).block();

    assertTrue(chain.invoked);
    assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
  }

  @Test
  void mcpRequestsAreTraced() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/mcp/invoke").header("X-MCP-ClientId", "client"));
    TrackingChain chain = new TrackingChain();

    filter.filter(exchange, chain).block();

    assertTrue(chain.invoked);
    assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
  }

  private static class TrackingChain implements WebFilterChain {
    private boolean invoked;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
      invoked = true;
      exchange.getResponse().setStatusCode(HttpStatus.OK);
      return exchange.getResponse().setComplete();
    }
  }
}
