package io.github.quota4j.quantityovertime;

import java.time.Instant;

public record QuantityOverTimeState(QuantityOverTimeLimit limit, long remainingTokens, Instant lastRefill) {

}
