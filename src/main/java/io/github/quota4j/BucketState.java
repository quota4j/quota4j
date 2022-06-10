package io.github.quota4j;

import io.github.quota4j.tokenbucket.Limit;

import java.time.Instant;

public record BucketState(Limit limit, long remainingTokens, Instant lastRefill) {
}
