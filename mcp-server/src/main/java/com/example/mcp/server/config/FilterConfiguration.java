package com.example.mcp.server.config;

import com.example.mcp.server.security.HmacAuthFilter;
import com.example.mcp.server.tracing.TracingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfiguration {
  @Bean
  public FilterRegistrationBean<HmacAuthFilter> hmacAuthFilterRegistration(HmacAuthFilter filter) {
    FilterRegistrationBean<HmacAuthFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns("/mcp/invoke");
    registration.setOrder(1);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<TracingFilter> tracingFilterRegistration(TracingFilter filter) {
    FilterRegistrationBean<TracingFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns("/mcp/*");
    registration.setOrder(0);
    return registration;
  }
}
