package com.myseotoolbox.quota4j.model;


import java.io.Serializable;

public record ResourceQuota(String resourceId, String quotaManagerClassName, Object initialState) implements Serializable { }
