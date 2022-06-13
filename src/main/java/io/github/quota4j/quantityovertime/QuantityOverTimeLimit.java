package io.github.quota4j.quantityovertime;

import java.time.Duration;

public record QuantityOverTimeLimit(long quantity, Duration duration) {
    public static QuantityOverTimeLimit limitOf(long quantity, Duration duration) {
        return new QuantityOverTimeLimit(quantity, duration);
    }
}