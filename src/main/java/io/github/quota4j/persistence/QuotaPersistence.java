package io.github.quota4j.persistence;

import io.github.quota4j.model.QuotaId;
import io.github.quota4j.model.QuotaState;

import java.util.Optional;

public interface QuotaPersistence {
    Optional<QuotaState> findById(QuotaId quotaId);

    QuotaState save(QuotaState quotaState);
}
