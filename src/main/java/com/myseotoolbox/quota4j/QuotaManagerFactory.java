package com.myseotoolbox.quota4j;

import com.myseotoolbox.quota4j.persistence.QuotaManagerStateChangeListener;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

public interface QuotaManagerFactory {
    QuotaManager<?> build(QuotaManagerStateChangeListener listener);
}
