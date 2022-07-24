package io.github.quota4j;

public class QuotaManagerNotRegisteredException extends RuntimeException {
    public QuotaManagerNotRegisteredException(String quotaManagerClassName) {
        super("Quota manager not registered: " + quotaManagerClassName);
    }
}
