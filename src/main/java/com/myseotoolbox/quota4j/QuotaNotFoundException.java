package com.myseotoolbox.quota4j;

public class QuotaNotFoundException extends RuntimeException {
    public QuotaNotFoundException(String quotaId) {
        super("Quota not found '" + quotaId + "'");
    }
}
