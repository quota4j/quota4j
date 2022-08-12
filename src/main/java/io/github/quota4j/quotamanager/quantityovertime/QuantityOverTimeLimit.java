package io.github.quota4j.quotamanager.quantityovertime;

import java.io.Serializable;
import java.time.Duration;

public record QuantityOverTimeLimit(long quantity, Duration duration) implements Serializable {
    public static QuantityOverTimeLimit limitOf(long quantity, Duration duration) {
        return new QuantityOverTimeLimit(quantity, duration);
    }
}