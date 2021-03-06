package io.github.quota4j.model;

public record UserQuotaId(String username, String resourceId) {
    public static UserQuotaId create(String username, String resourceId) {
        return new UserQuotaId(username, resourceId);
    }
}
