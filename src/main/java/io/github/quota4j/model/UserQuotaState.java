package io.github.quota4j.model;

public record UserQuotaState(UserQuotaId id, String quotaManagerClassName, Object state) {
}
