package com.example.mcp.server.tracing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TracingFilterTest {

  private final TracingFilter filter = new TracingFilter();

  @Test
  void shouldNotFilterSkipsNonMcpPaths() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/health");

    assertTrue(filter.shouldNotFilter(request));
  }

  @Test
  void filterCreatesSpanAroundRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/mcp/invoke");
    request.addHeader("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse)
              throws java.io.IOException, ServletException {
            servletRequest.setAttribute("mcp.tool.name", "tool");
            super.doFilter(servletRequest, servletResponse);
          }
        };

    filter.doFilterInternal(request, response, chain);

    assertFalse(response.isCommitted());
  }
}
