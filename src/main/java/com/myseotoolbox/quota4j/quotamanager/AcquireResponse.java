package com.myseotoolbox.quota4j.quotamanager;

public record AcquireResponse<T>(T state, boolean result) {
    public static <T> AcquireResponse<T> grantedWithState(T newState) {
        return new AcquireResponse<>(newState, true);
    }
    public static <T> AcquireResponse<T> declinedWithState(T newState) {
        return new AcquireResponse<>(newState, false);
    }
}
