package io.github.quota4j.model;


import java.io.Serializable;

public record ResourceQuota(String id, String quotaManagerClassName, Object initialState) implements Serializable { }
