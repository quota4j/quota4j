package com.myseotoolbox.quota4j.model;

import java.io.Serializable;

public record QuotaStateId(String ownerId, String quotaId)  implements Serializable {
    public static QuotaStateId create(String ownerId, String quotaId) {
        return new QuotaStateId(ownerId, quotaId);
    }
}
