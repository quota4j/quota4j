package io.github.quota4j.limit;


public record ResourceQuota(String id, String quotaManagerClassName, Object initialState) {
}
