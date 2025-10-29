package com.example.mcp.client.invocation;

import com.example.mcp.common.StdResponse;
import java.util.Collections;
import java.util.List;

/**
 * Container for multiple invocation samples.
 */
public final class SamplingResult<TResponse> {

  private final List<StdResponse<TResponse>> samples;

  public SamplingResult(List<StdResponse<TResponse>> samples) {
    this.samples = List.copyOf(samples);
  }

  public List<StdResponse<TResponse>> getSamples() {
    return Collections.unmodifiableList(samples);
  }

  public StdResponse<TResponse> primary() {
    return samples.isEmpty() ? null : samples.get(0);
  }
}
