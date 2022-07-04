package io.github.quota4j.model;

public interface UserQuotaState {
    UserQuotaId id();

    String quotaManagerClassName();

    Object state();
}
