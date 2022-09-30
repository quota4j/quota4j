package com.myseotoolbox.quota4j;

import com.myseotoolbox.quota4j.model.QuotaId;
import com.myseotoolbox.quota4j.model.QuotaState;
import com.myseotoolbox.quota4j.model.ResourceQuota;
import com.myseotoolbox.quota4j.persistence.QuotaStatePersistence;
import com.myseotoolbox.quota4j.persistence.ResourceQuotaPersistence;
import com.myseotoolbox.quota4j.quotamanager.AcquireResponse;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

import java.util.HashMap;
import java.util.Optional;

public class QuotaService {
    private final ResourceQuotaPersistence resourceQuotaPersistence;
    private final QuotaStatePersistence quotaStatePersistence;
    private final HashMap<String, QuotaManagerFactory> quotaManagerFactories = new HashMap<>();
    private final HashMap<QuotaId, QuotaManager<?>> quotaManagers = new HashMap<>();

    public QuotaService(ResourceQuotaPersistence resourceQuotaPersistence, QuotaStatePersistence quotaStatePersistence) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
        this.quotaStatePersistence = quotaStatePersistence;
    }

    public AcquireResponse<?> tryAcquire(String ownerId, String resourceId, long quantity) throws QuotaManagerNotRegisteredException {
        QuotaId quotaId = QuotaId.create(ownerId, resourceId);
        QuotaState quotaState = getQuotaState(quotaId);
        AcquireResponse<?> response = getQuotaManager(quotaId, quotaState.quotaManagerClassName())
                .tryAcquire(quotaState.currentState(), quantity);
        quotaStatePersistence.save(quotaState.withUpdatedState(response.state()));
        return response;
    }

    public Object getQuotaState(String ownerId, String resourceId) {
        QuotaId quotaId = QuotaId.create(ownerId, resourceId);
        QuotaState quotaState = getQuotaState(quotaId);
        Object newState = getQuotaManager(quotaId, quotaState.quotaManagerClassName())
                .getCurrentState(quotaState.currentState());
        quotaStatePersistence.save(quotaState.withUpdatedState(newState));
        return newState;
    }

    public void registerQuotaManagerFactory(String className, QuotaManagerFactory quotaManagerFactory) {
        quotaManagerFactories.computeIfAbsent(className, ignored -> quotaManagerFactory);
    }

    private QuotaState getQuotaState(QuotaId quotaId) {
        return quotaStatePersistence
                .findById(quotaId)
                .orElseGet(() -> createFromResourceQuota(quotaId));
    }

    private QuotaState createFromResourceQuota(QuotaId quotaId) {
        ResourceQuota resourceQuota = resourceQuotaPersistence.findById(quotaId.resourceId()).orElseThrow(() -> new ResourceQuotaNotFoundException(quotaId.resourceId()));
        return new QuotaState(quotaId, resourceQuota.quotaManagerClassName(), resourceQuota.initialState());
    }

    private QuotaManager<Object> getQuotaManager(QuotaId quotaId, String quotaManagerClassName) {
        QuotaManager<?> quotaManager = quotaManagers.computeIfAbsent(quotaId, it -> {
            QuotaManagerFactory quotaManagerFactory = Optional.ofNullable(quotaManagerFactories.get(quotaManagerClassName)).orElseThrow(() -> new QuotaManagerNotRegisteredException(quotaManagerClassName));
            return quotaManagerFactory.build(state -> quotaStatePersistence.save(new QuotaState(quotaId, quotaManagerClassName, state)));
        });
        return (QuotaManager<Object>) quotaManager;
    }


}
