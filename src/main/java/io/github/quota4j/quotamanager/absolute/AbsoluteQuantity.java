package io.github.quota4j.quotamanager.absolute;

import io.github.quota4j.quotamanager.QuotaManager;

public final class AbsoluteQuantity implements QuotaManager {
    private final long maxQuantity;

    public AbsoluteQuantity(long maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    @Override
    public boolean tryConsume(long quantity) {
        return false;
    }

    @Override
    public long getRemaining() {
        return 0;
    }
}
