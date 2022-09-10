package com.myseotoolbox.quota4j.model;

import java.io.Serializable;

public record QuotaId(String ownerId, String resourceId)  implements Serializable {
    public static QuotaId create(String ownerId, String resourceId) {
        return new QuotaId(ownerId, resourceId);
    }
}
