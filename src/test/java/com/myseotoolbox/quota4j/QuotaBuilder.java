package com.myseotoolbox.quota4j;


import com.myseotoolbox.quota4j.model.Quota;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

public class QuotaBuilder {

    private final String quotaId;
    private String quotaManagerClassName;
    private Object defaultState;

    QuotaBuilder(String quotaId) {
        this.quotaId = quotaId;
    }

    public static QuotaBuilder createWithQuotaId(String quotaId) {
        return new QuotaBuilder(quotaId);
    }

    public <T extends QuotaManager> QuotaBuilder withQuotaManager(String className) {
        this.quotaManagerClassName = className;
        return this;
    }

    public QuotaBuilder defaultState(Object defaultState) {
        this.defaultState = defaultState;
        return this;
    }

    public Quota build() {
        return new Quota(quotaId, quotaManagerClassName, defaultState);
    }
}
