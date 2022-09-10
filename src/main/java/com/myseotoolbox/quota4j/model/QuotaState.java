package com.myseotoolbox.quota4j.model;

import java.io.Serializable;

public record QuotaState(QuotaId id, String quotaManagerClassName, Object currentState) implements Serializable {

}