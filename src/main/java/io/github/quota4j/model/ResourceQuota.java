package io.github.quota4j.model;


public record ResourceQuota(String id, String quotaManagerClassName, Object initialState) {
}
