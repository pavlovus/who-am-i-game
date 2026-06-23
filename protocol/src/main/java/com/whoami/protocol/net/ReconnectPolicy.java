package com.whoami.protocol.net;

/**
 * Stateful exponential-backoff policy used by the client to recover from a
 * dropped connection. Delays grow geometrically up to a cap; attempts are
 * limited so the client eventually gives up instead of spinning forever.
 */
public final class ReconnectPolicy {

    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;

    private int attempts;

    public ReconnectPolicy(int maxAttempts, long baseDelayMs, long maxDelayMs) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (baseDelayMs <= 0 || maxDelayMs < baseDelayMs) {
            throw new IllegalArgumentException("invalid delay bounds");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    public static ReconnectPolicy defaultPolicy() {
        return new ReconnectPolicy(5, 500, 8000);
    }

    public boolean canRetry() {
        return attempts < maxAttempts;
    }

    /**
     * Registers one more attempt and returns how long to wait before it.
     * Delay for attempt n (1-based) is base * 2^(n-1), capped at maxDelayMs.
     */
    public long nextDelayMs() {
        if (!canRetry()) {
            throw new IllegalStateException("No retries left");
        }
        long delay = baseDelayMs << attempts;
        if (delay > maxDelayMs || delay < 0) {
            delay = maxDelayMs;
        }
        attempts++;
        return delay;
    }

    public void reset() {
        attempts = 0;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
