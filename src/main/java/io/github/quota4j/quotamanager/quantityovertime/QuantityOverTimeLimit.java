package io.github.quota4j.quotamanager.quantityovertime;

import java.time.Duration;

public record QuantityOverTimeLimit(long quantity, Duration duration) {
    public static QuantityOverTimeLimit limitOf(long quantity, Duration duration) {
        return new QuantityOverTimeLimit(quantity, duration);
    }
}