package io.github.quota4j;

import java.util.Optional;

public interface BucketPersistence {
    Optional<BucketState> getBucketState();

    BucketState save(BucketState entity);
}
