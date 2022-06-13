package io.github.quota4j.quotamanager;

public interface QuotaManager {

    boolean tryConsume(long quantity);

    long getRemaining();
}
