package io.github.quota4j.persistence;

import io.github.quota4j.model.ResourceQuota;

import java.util.Optional;

public interface ResourceQuotaPersistence {
    ResourceQuota save(ResourceQuota resourceQuota);

    Optional<ResourceQuota> findById(String resourceId);
}
