package io.github.quota4j.utils;

import io.github.quota4j.quotamanager.QuotaManager;
import io.github.quota4j.model.ResourceQuota;

public class ResourceQuotaBuilder {

    private final String resourceId;
    private Class<?> quotaManager;
    private Object initialState;

    ResourceQuotaBuilder(String resourceId) {
        this.resourceId = resourceId;
    }

    public static ResourceQuotaBuilder createWithResourceId(String resourceId) {
        return new ResourceQuotaBuilder(resourceId);
    }

    public <T extends QuotaManager> ResourceQuotaBuilder withQuotaManager(Class<T> quotaManager) {
        this.quotaManager = quotaManager;
        return this;
    }

    public ResourceQuotaBuilder initialState(Object initialState) {
        this.initialState = initialState;
        return this;
    }

    public ResourceQuota build() {
        return new ResourceQuota(resourceId, quotaManager.getName(), initialState);
    }
}
