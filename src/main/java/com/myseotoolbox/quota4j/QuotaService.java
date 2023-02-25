package com.myseotoolbox.quota4j;

import com.myseotoolbox.quota4j.model.QuotaStateId;
import com.myseotoolbox.quota4j.model.QuotaState;
import com.myseotoolbox.quota4j.model.Quota;
import com.myseotoolbox.quota4j.persistence.QuotaStatePersistence;
import com.myseotoolbox.quota4j.persistence.QuotaPersistence;
import com.myseotoolbox.quota4j.quotamanager.AcquireResponse;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

import java.util.HashMap;
import java.util.Optional;

public class QuotaService {
    private final QuotaPersistence quotaPersistence;
    private final QuotaStatePersistence quotaStatePersistence;
    private final HashMap<String, QuotaManagerFactory> quotaManagerFactories = new HashMap<>();
    private final HashMap<QuotaStateId, QuotaManager<?>> quotaManagers = new HashMap<>();

    public QuotaService(QuotaPersistence quotaPersistence, QuotaStatePersistence quotaStatePersistence) {
        this.quotaPersistence = quotaPersistence;
        this.quotaStatePersistence = quotaStatePersistence;
    }

    public AcquireResponse<?> tryAcquire(String ownerId, String quotaId, long quantity) throws QuotaManagerNotRegisteredException {
        QuotaStateId quotaStateId = QuotaStateId.create(ownerId, quotaId);
        QuotaState quotaState = getQuotaState(quotaStateId);
        AcquireResponse<?> response = getQuotaManager(quotaStateId, quotaState.quotaManagerClassName())
                .tryAcquire(quotaState.currentState(), quantity);
        quotaStatePersistence.save(quotaState.withUpdatedState(response.state()));
        return response;
    }

    public Object getQuotaState(String ownerId, String quotaId) {
        QuotaStateId quotaStateId = QuotaStateId.create(ownerId, quotaId);
        QuotaState quotaState = getQuotaState(quotaStateId);
        Object newState = getQuotaManager(quotaStateId, quotaState.quotaManagerClassName())
                .getCurrentState(quotaState.currentState());
        quotaStatePersistence.save(quotaState.withUpdatedState(newState));
        return newState;
    }

    public void registerQuotaManagerFactory(String className, QuotaManagerFactory quotaManagerFactory) {
        quotaManagerFactories.put(className, quotaManagerFactory);
    }

    private QuotaState getQuotaState(QuotaStateId quotaStateId) {
        return quotaStatePersistence
                .findById(quotaStateId)
                .orElseGet(() -> createFor(quotaStateId));
    }

    private QuotaState createFor(QuotaStateId quotaStateId) {
        Quota quota = quotaPersistence.findById(quotaStateId.quotaId()).orElseThrow(() -> new QuotaNotFoundException(quotaStateId.quotaId()));
        return new QuotaState(quotaStateId, quota.quotaManagerClassName(), quota.defaultState());
    }

    private QuotaManager<Object> getQuotaManager(QuotaStateId quotaStateId, String quotaManagerClassName) {
        QuotaManager<?> quotaManager = quotaManagers.computeIfAbsent(quotaStateId, it -> {
            QuotaManagerFactory quotaManagerFactory =
                    Optional.ofNullable(quotaManagerFactories.get(quotaManagerClassName))
                            .orElseThrow(() -> new QuotaManagerNotRegisteredException(quotaManagerClassName));
            return quotaManagerFactory.build();
        });
        return (QuotaManager<Object>) quotaManager;
    }


}
