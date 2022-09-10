package com.myseotoolbox.quota4j.quotamanager;

public interface QuotaManager<T> {

    boolean tryConsume(T state, long quantity);

    long getRemaining(T state);
}
