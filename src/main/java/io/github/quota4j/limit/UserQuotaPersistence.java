package io.github.quota4j.limit;

import java.util.Optional;

public interface UserQuotaPersistence {
    Optional<UserQuotaState> findById(UserQuotaId userQuotaId);

    UserQuotaState save(UserQuotaState userQuotaState);
}
