package com.myseotoolbox.quota4j;


import com.myseotoolbox.quota4j.model.ResourceQuota;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

public class ResourceQuotaBuilder {

    private final String resourceId;
    private String quotaManagerClassName;
    private Object initialState;

    ResourceQuotaBuilder(String resourceId) {
        this.resourceId = resourceId;
    }

    public static ResourceQuotaBuilder createWithResourceId(String resourceId) {
        return new ResourceQuotaBuilder(resourceId);
    }

    public <T extends QuotaManager> ResourceQuotaBuilder withQuotaManager(String className) {
        this.quotaManagerClassName = className;
        return this;
    }

    public ResourceQuotaBuilder initialState(Object initialState) {
        this.initialState = initialState;
        return this;
    }

    public ResourceQuota build() {
        return new ResourceQuota(resourceId, quotaManagerClassName, initialState);
    }
}