package com.myseotoolbox.quota4j.persistence;

import com.myseotoolbox.quota4j.model.Quota;

import java.util.Optional;

public interface QuotaPersistence {
    Quota save(Quota quota);

    Optional<Quota> findById(String id);
}
