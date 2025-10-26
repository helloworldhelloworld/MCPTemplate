package com.example.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.server.security.HmacAuthFilter;
import com.example.mcp.server.tracing.TracingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class FilterConfigurationTest {

  private final FilterConfiguration configuration = new FilterConfiguration();

  @Test
  void hmacFilterRegistrationRegistersInvokePath() {
    HmacAuthFilter filter = new HmacAuthFilter();
    FilterRegistrationBean<HmacAuthFilter> bean = configuration.hmacAuthFilterRegistration(filter);

    assertEquals(1, bean.getOrder());
    assertTrue(bean.getUrlPatterns().contains("/mcp/invoke"));
  }

  @Test
  void tracingFilterRegistrationCoversAllMcpPaths() {
    TracingFilter filter = new TracingFilter();
    FilterRegistrationBean<TracingFilter> bean = configuration.tracingFilterRegistration(filter);

    assertEquals(0, bean.getOrder());
    assertTrue(bean.getUrlPatterns().contains("/mcp/*"));
  }
}
