package io.github.quota4j.limit;

public interface QuotaPersistence {
    void save(Object state);
}
