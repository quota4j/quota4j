package io.github.quota4j;

import io.github.quota4j.model.ResourceQuota;

public record ResourceQuotaImpl(String id, String quotaManagerClassName, Object initialState) implements ResourceQuota { }
