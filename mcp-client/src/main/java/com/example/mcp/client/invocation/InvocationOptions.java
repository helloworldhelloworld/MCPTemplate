package com.example.mcp.client.invocation;

import java.util.Optional;

/**
 * Options controlling invocation features such as sampling, elicitation and logging.
 */
public final class InvocationOptions<TRequest> {

  private final int samples;
  private final int maxElicitationRounds;
  private final ElicitationStrategy<TRequest> elicitationStrategy;
  private final InvocationLogger logger;
  private final ProgressListener progressListener;

  private InvocationOptions(Builder<TRequest> builder) {
    this.samples = builder.samples;
    this.maxElicitationRounds = builder.maxElicitationRounds;
    this.elicitationStrategy = builder.elicitationStrategy;
    this.logger = builder.logger;
    this.progressListener = builder.progressListener;
  }

  public int getSamples() {
    return samples;
  }

  public int getMaxElicitationRounds() {
    return maxElicitationRounds;
  }

  public Optional<ElicitationStrategy<TRequest>> getElicitationStrategy() {
    return Optional.ofNullable(elicitationStrategy);
  }

  public Optional<InvocationLogger> getLogger() {
    return Optional.ofNullable(logger);
  }

  public Optional<ProgressListener> getProgressListener() {
    return Optional.ofNullable(progressListener);
  }

  public static <TRequest> Builder<TRequest> builder() {
    return new Builder<>();
  }

  public static final class Builder<TRequest> {
    private int samples = 1;
    private int maxElicitationRounds = 3;
    private ElicitationStrategy<TRequest> elicitationStrategy;
    private InvocationLogger logger;
    private ProgressListener progressListener;

    public Builder<TRequest> samples(int samples) {
      this.samples = Math.max(1, samples);
      return this;
    }

    public Builder<TRequest> maxElicitationRounds(int rounds) {
      this.maxElicitationRounds = Math.max(1, rounds);
      return this;
    }

    public Builder<TRequest> elicitationStrategy(ElicitationStrategy<TRequest> strategy) {
      this.elicitationStrategy = strategy;
      return this;
    }

    public Builder<TRequest> logger(InvocationLogger logger) {
      this.logger = logger;
      return this;
    }

    public Builder<TRequest> progressListener(ProgressListener listener) {
      this.progressListener = listener;
      return this;
    }

    public InvocationOptions<TRequest> build() {
      return new InvocationOptions<>(this);
    }
  }
}
