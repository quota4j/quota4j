package io.github.quota4j.tokenbucket;

import java.time.Instant;

public record BucketState(Limit limit, long remainingTokens, Instant lastRefill) {
}
