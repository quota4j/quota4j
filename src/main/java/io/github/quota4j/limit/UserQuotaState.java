package io.github.quota4j.limit;

public record UserQuotaState(UserQuotaId id, String quotaManagerClassName, Object state) {
}
