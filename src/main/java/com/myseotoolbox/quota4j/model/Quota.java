package com.myseotoolbox.quota4j.model;


import java.io.Serializable;

public record Quota(String id, String quotaManagerClassName, Object defaultState) implements Serializable { }
