package io.github.quota4j.limit;

import io.github.quota4j.QuotaManager;
import io.github.quota4j.model.ResourceQuota;
import io.github.quota4j.persistence.ResourceQuotaPersistence;

public class ResourceQuotaBuilder {
    private final ResourceQuotaPersistence resourceQuotaPersistence;
    private final String resourceId;
    private Class<?> quotaManager;
    private Object initialState;

    ResourceQuotaBuilder(ResourceQuotaPersistence resourceQuotaPersistence, String resourceId) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
        this.resourceId = resourceId;
    }

    public <T extends QuotaManager> ResourceQuotaBuilder withQuotaManager(Class<T> quotaManager) {
        this.quotaManager = quotaManager;
        return this;
    }

    public ResourceQuotaBuilder initialState(Object initialState) {
        this.initialState = initialState;
        return this;
    }

    public void save() {
        resourceQuotaPersistence.save(new ResourceQuota(resourceId, quotaManager.getName(), initialState));
    }
}
