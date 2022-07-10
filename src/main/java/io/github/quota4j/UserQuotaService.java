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
    private final HashMap<String, QuotaManager<Object>> quotaManagers = new HashMap<>();

    public UserQuotaService(ResourceQuotaPersistence resourceQuotaPersistence,
                            UserQuotaPersistence userQuotaPersistence) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
        this.userQuotaPersistence = userQuotaPersistence;
    }

    public boolean tryAcquire(String username, String resourceId, int quantity) {
        UserQuotaId userQuotaId = UserQuotaId.create(username, resourceId);

        QuotaManager<Object> quotaManager = quotaManagers.get(resourceId);


        UserQuotaState userQuotaState = userQuotaPersistence
                .findById(userQuotaId)
                .orElseGet(() -> {
                    ResourceQuota resourceQuota = resourceQuotaPersistence.findById(userQuotaId.resourceId()).orElseThrow();
                    return new UserQuotaStateImpl(UserQuotaId.create(username, resourceId), resourceQuota.quotaManagerClassName(), resourceQuota.initialState());
                });

        return quotaManager.tryConsume(userQuotaState.state(), quantity);
    }

    public QuotaManager<Object> registerQuotaManager(String resourceId, QuotaManager<Object> quotaManager) {
        return quotaManagers.put(resourceId, quotaManager);
    }

    private record UserQuotaStateImpl(UserQuotaId id, String quotaManagerClassName,
                                      Object state) implements UserQuotaState {
    }

}
