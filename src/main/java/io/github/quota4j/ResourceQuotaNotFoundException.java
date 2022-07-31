package io.github.quota4j;

public class ResourceQuotaNotFoundException extends RuntimeException {
    public ResourceQuotaNotFoundException(String resourceId) {
        super("ResourceQuota not found '" + resourceId + "'");
    }
}
