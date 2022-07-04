package io.github.quota4j.model;


public interface ResourceQuota{
    String id();
    String quotaManagerClassName();
    Object initialState();
}
