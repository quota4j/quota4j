package io.github.quota4j.limit;

import io.github.quota4j.persistence.ResourceQuotaPersistence;

public class ResourceQuotaFactory {
    private final ResourceQuotaPersistence resourceQuotaPersistence;

    public ResourceQuotaFactory(ResourceQuotaPersistence resourceQuotaPersistence) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
    }

    public ResourceQuotaBuilder createWithResourceId(String resourceId) {
        return new ResourceQuotaBuilder(resourceQuotaPersistence, resourceId);
    }
}
