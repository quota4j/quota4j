package io.github.quota4j;

import io.github.quota4j.model.ResourceQuota;
import io.github.quota4j.model.UserQuotaId;
import io.github.quota4j.model.UserQuotaState;
import io.github.quota4j.persistence.ResourceQuotaPersistence;
import io.github.quota4j.persistence.UserQuotaPersistence;
import io.github.quota4j.quotamanager.QuotaManager;

import java.util.HashMap;

public class UserQuotaService {
    private final ResourceQuotaPersistence resourceQuotaPersistence;
    private final UserQuotaPersistence userQuotaPersistence;
    private final HashMap<String, QuotaManagerFactory> quotaManagerFactories = new HashMap<>();
    private final HashMap<UserQuotaId, QuotaManager<?>> quotaManagers = new HashMap<>();

    public UserQuotaService(ResourceQuotaPersistence resourceQuotaPersistence, UserQuotaPersistence userQuotaPersistence) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
        this.userQuotaPersistence = userQuotaPersistence;
    }

    public boolean tryAcquire(String username, String resourceId, int quantity) {
        UserQuotaId userQuotaId = UserQuotaId.create(username, resourceId);

        UserQuotaState userQuotaState = userQuotaPersistence
                .findById(userQuotaId)
                .orElseGet(() -> {
                    ResourceQuota resourceQuota = resourceQuotaPersistence.findById(userQuotaId.resourceId()).orElseThrow();
                    return new UserQuotaStateImpl(UserQuotaId.create(username, resourceId), resourceQuota.quotaManagerClassName(), resourceQuota.initialState());
                });

        return getQuotaManager(userQuotaId, userQuotaState)
                .tryConsume(userQuotaState.state(), quantity);
    }

    private QuotaManager<Object> getQuotaManager(UserQuotaId userQuotaId, UserQuotaState userQuotaState) {
        QuotaManager<?> quotaManager = quotaManagers.computeIfAbsent(userQuotaId, it -> {
            QuotaManagerFactory quotaManagerFactory = quotaManagerFactories.get(userQuotaState.quotaManagerClassName());
            return quotaManagerFactory.build(state -> userQuotaPersistence.save(new UserQuotaStateImpl(userQuotaId, userQuotaState.quotaManagerClassName(), state)));
        });
        return (QuotaManager<Object>) quotaManager;
    }

    public void registerQuotaManager(String className, QuotaManagerFactory quotaManagerFactory) {
        quotaManagerFactories.computeIfAbsent(className, s -> quotaManagerFactory);
    }

    private record UserQuotaStateImpl(UserQuotaId id, String quotaManagerClassName,
                                      Object state) implements UserQuotaState {
    }

}
