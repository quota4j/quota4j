package com.myseotoolbox.quota4j.persistence;

import com.myseotoolbox.quota4j.model.QuotaState;
import com.myseotoolbox.quota4j.model.QuotaStateId;

import java.util.Optional;

public interface QuotaStatePersistence {
    Optional<QuotaState> findById(QuotaStateId quotaStateId);

    QuotaState save(QuotaState quotaState);
}
