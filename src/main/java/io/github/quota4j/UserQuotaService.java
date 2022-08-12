package io.github.quota4j;

import io.github.quota4j.model.ResourceQuota;
import io.github.quota4j.model.UserQuotaId;
import io.github.quota4j.model.UserQuotaState;
import io.github.quota4j.persistence.ResourceQuotaPersistence;
import io.github.quota4j.persistence.UserQuotaPersistence;
import io.github.quota4j.quotamanager.QuotaManager;

import java.util.HashMap;
import java.util.Optional;

public class UserQuotaService {
    private final ResourceQuotaPersistence resourceQuotaPersistence;
    private final UserQuotaPersistence userQuotaPersistence;
    private final HashMap<String, QuotaManagerFactory> quotaManagerFactories = new HashMap<>();
    private final HashMap<UserQuotaId, QuotaManager<?>> quotaManagers = new HashMap<>();

    public UserQuotaService(ResourceQuotaPersistence resourceQuotaPersistence, UserQuotaPersistence userQuotaPersistence) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
        this.userQuotaPersistence = userQuotaPersistence;
    }

    public boolean tryAcquire(String username, String resourceId, int quantity) throws QuotaManagerNotRegisteredException {
        UserQuotaId userQuotaId = UserQuotaId.create(username, resourceId);

        UserQuotaState userQuotaState = userQuotaPersistence
                .findById(userQuotaId)
                .orElseGet(() -> createFromResourceQuota(userQuotaId));

        return getQuotaManager(userQuotaId, userQuotaState)
                .tryConsume(userQuotaState.currentState(), quantity);
    }

    public void registerQuotaManagerFactory(String className, QuotaManagerFactory quotaManagerFactory) {
        quotaManagerFactories.computeIfAbsent(className, ignored -> quotaManagerFactory);
    }

    private UserQuotaState createFromResourceQuota(UserQuotaId userQuotaId) {
        ResourceQuota resourceQuota = resourceQuotaPersistence.findById(userQuotaId.resourceId()).orElseThrow(() -> new ResourceQuotaNotFoundException(userQuotaId.resourceId()));
        return new UserQuotaState(userQuotaId, resourceQuota.quotaManagerClassName(), resourceQuota.initialState());
    }

    private QuotaManager<Object> getQuotaManager(UserQuotaId userQuotaId, UserQuotaState userQuotaState) {
        QuotaManager<?> quotaManager = quotaManagers.computeIfAbsent(userQuotaId, it -> {
            String quotaManagerClassName = userQuotaState.quotaManagerClassName();
            QuotaManagerFactory quotaManagerFactory = Optional.ofNullable(quotaManagerFactories.get(quotaManagerClassName)).orElseThrow(() -> new QuotaManagerNotRegisteredException(quotaManagerClassName));
            return quotaManagerFactory.build(state -> userQuotaPersistence.save(new UserQuotaState(userQuotaId, quotaManagerClassName, state)));
        });
        return (QuotaManager<Object>) quotaManager;
    }



}
