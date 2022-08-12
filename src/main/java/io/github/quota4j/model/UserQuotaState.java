package io.github.quota4j.model;

import java.io.Serializable;

public record UserQuotaState(UserQuotaId id, String quotaManagerClassName, Object currentState) implements Serializable {

}