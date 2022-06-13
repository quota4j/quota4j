package io.github.quota4j;

public interface QuotaManager {

    boolean tryConsume(long quantity);

    long getRemaining();
}
