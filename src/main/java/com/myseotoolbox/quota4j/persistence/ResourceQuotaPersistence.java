package com.myseotoolbox.quota4j.persistence;

import com.myseotoolbox.quota4j.model.ResourceQuota;

import java.util.Optional;

public interface ResourceQuotaPersistence {
    ResourceQuota save(ResourceQuota resourceQuota);

    Optional<ResourceQuota> findById(String resourceId);
}
