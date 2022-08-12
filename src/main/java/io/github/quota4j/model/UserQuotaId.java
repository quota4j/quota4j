package io.github.quota4j.model;

import java.io.Serializable;

public record UserQuotaId(String username, String resourceId)  implements Serializable {
    public static UserQuotaId create(String username, String resourceId) {
        return new UserQuotaId(username, resourceId);
    }
}
