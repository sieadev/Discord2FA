package dev.siea.discord2fa.common.testutil;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/** Polls a condition until it holds; used to wait for work handed to the server's DB thread. */
public final class Await {

    private Await() {
    }

    public static void until(String description, BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted waiting for: " + description);
            }
        }
        fail("Timed out waiting for: " + description);
    }
}
