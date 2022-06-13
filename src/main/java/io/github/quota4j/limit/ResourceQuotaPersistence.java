package io.github.quota4j.limit;

import java.util.Optional;

public interface ResourceQuotaPersistence {
    ResourceQuota save(ResourceQuota resourceQuota);

    Optional<ResourceQuota> findById(String resourceId);
}
