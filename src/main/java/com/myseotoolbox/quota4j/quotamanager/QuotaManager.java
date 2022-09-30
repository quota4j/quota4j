package com.myseotoolbox.quota4j.quotamanager;

public interface QuotaManager<T> {
    AcquireResponse tryAcquire(T state, long quantity);

    T getCurrentState(T currentState);
}
