package io.github.quota4j;


import io.github.quota4j.quotamanager.QuotaManager;

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

    public ResourceQuotaImpl build() {
        return new ResourceQuotaImpl(resourceId, quotaManagerClassName, initialState);
    }
}
