package io.github.quota4j;

import io.github.quota4j.persistence.QuotaManagerStateChangeListener;
import io.github.quota4j.quotamanager.QuotaManager;

public interface QuotaManagerFactory {
    QuotaManager<?> build(QuotaManagerStateChangeListener listener);
}
