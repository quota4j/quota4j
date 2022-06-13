package io.github.quota4j.persistence;

import io.github.quota4j.model.UserQuotaId;
import io.github.quota4j.model.UserQuotaState;

import java.util.Optional;

public interface UserQuotaPersistence {
    Optional<UserQuotaState> findById(UserQuotaId userQuotaId);

    UserQuotaState save(UserQuotaState userQuotaState);
}
