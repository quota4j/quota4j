package io.github.quota4j;

public interface Quota {
    boolean tryConsume(long quantity);

    long getRemaining();
}
