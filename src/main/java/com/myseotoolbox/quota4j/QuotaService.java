package com.myseotoolbox.quota4j;

import com.myseotoolbox.quota4j.persistence.ResourceQuotaPersistence;
import com.myseotoolbox.quota4j.model.ResourceQuota;
import com.myseotoolbox.quota4j.model.QuotaId;
import com.myseotoolbox.quota4j.model.QuotaState;
import com.myseotoolbox.quota4j.persistence.QuotaPersistence;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

import java.util.HashMap;
import java.util.Optional;

public class QuotaService {
    private final ResourceQuotaPersistence resourceQuotaPersistence;
    private final QuotaPersistence quotaPersistence;
    private final HashMap<String, QuotaManagerFactory> quotaManagerFactories = new HashMap<>();
    private final HashMap<QuotaId, QuotaManager<?>> quotaManagers = new HashMap<>();

    public QuotaService(ResourceQuotaPersistence resourceQuotaPersistence, QuotaPersistence quotaPersistence) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
        this.quotaPersistence = quotaPersistence;
    }

    public boolean tryAcquire(String ownerId, String resourceId, int quantity) throws QuotaManagerNotRegisteredException {
        QuotaId quotaId = QuotaId.create(ownerId, resourceId);

        QuotaState quotaState = quotaPersistence
                .findById(quotaId)
                .orElseGet(() -> createFromResourceQuota(quotaId));

        return getQuotaManager(quotaId, quotaState)
                .tryConsume(quotaState.currentState(), quantity);
    }

    public void registerQuotaManagerFactory(String className, QuotaManagerFactory quotaManagerFactory) {
        quotaManagerFactories.computeIfAbsent(className, ignored -> quotaManagerFactory);
    }

    private QuotaState createFromResourceQuota(QuotaId quotaId) {
        ResourceQuota resourceQuota = resourceQuotaPersistence.findById(quotaId.resourceId()).orElseThrow(() -> new ResourceQuotaNotFoundException(quotaId.resourceId()));
        return new QuotaState(quotaId, resourceQuota.quotaManagerClassName(), resourceQuota.initialState());
    }

    private QuotaManager<Object> getQuotaManager(QuotaId quotaId, QuotaState quotaState) {
        QuotaManager<?> quotaManager = quotaManagers.computeIfAbsent(quotaId, it -> {
            String quotaManagerClassName = quotaState.quotaManagerClassName();
            QuotaManagerFactory quotaManagerFactory = Optional.ofNullable(quotaManagerFactories.get(quotaManagerClassName)).orElseThrow(() -> new QuotaManagerNotRegisteredException(quotaManagerClassName));
            return quotaManagerFactory.build(state -> quotaPersistence.save(new QuotaState(quotaId, quotaManagerClassName, state)));
        });
        return (QuotaManager<Object>) quotaManager;
    }



}
