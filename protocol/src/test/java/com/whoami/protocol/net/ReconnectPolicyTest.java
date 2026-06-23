package com.whoami.protocol.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReconnectPolicyTest {

    @Test
    public void delaysGrowExponentiallyAndAreCapped() {
        ReconnectPolicy policy = new ReconnectPolicy(5, 500, 3000);
        assertEquals(500, policy.nextDelayMs());
        assertEquals(1000, policy.nextDelayMs());
        assertEquals(2000, policy.nextDelayMs());
        assertEquals(3000, policy.nextDelayMs(), "delay must be capped at maxDelayMs");
        assertEquals(3000, policy.nextDelayMs());
    }

    @Test
    public void stopsAfterMaxAttempts() {
        ReconnectPolicy policy = new ReconnectPolicy(2, 100, 1000);
        assertTrue(policy.canRetry());
        policy.nextDelayMs();
        policy.nextDelayMs();
        assertFalse(policy.canRetry());
        assertThrows(IllegalStateException.class, policy::nextDelayMs);
    }

    @Test
    public void resetRestoresAttempts() {
        ReconnectPolicy policy = new ReconnectPolicy(3, 100, 1000);
        policy.nextDelayMs();
        policy.reset();
        assertEquals(0, policy.getAttempts());
        assertEquals(100, policy.nextDelayMs());
    }

    @Test
    public void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new ReconnectPolicy(0, 100, 1000));
        assertThrows(IllegalArgumentException.class, () -> new ReconnectPolicy(3, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new ReconnectPolicy(3, 1000, 100));
    }
}
