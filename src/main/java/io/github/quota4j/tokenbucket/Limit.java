package io.github.quota4j.tokenbucket;

import java.time.Duration;

public record Limit(long quantity, Duration duration) { }