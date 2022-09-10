package com.myseotoolbox.quota4j.persistence;

import com.myseotoolbox.quota4j.model.QuotaState;
import com.myseotoolbox.quota4j.model.QuotaId;

import java.util.Optional;

public interface QuotaPersistence {
    Optional<QuotaState> findById(QuotaId quotaId);

    QuotaState save(QuotaState quotaState);
}
