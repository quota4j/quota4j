package io.github.quota4j;

import io.github.quota4j.quotamanager.QuotaManager;
import io.github.quota4j.model.ResourceQuota;
import io.github.quota4j.model.UserQuotaId;
import io.github.quota4j.model.UserQuotaState;
import io.github.quota4j.persistence.QuotaPersistence;
import io.github.quota4j.persistence.ResourceQuotaPersistence;
import io.github.quota4j.persistence.UserQuotaPersistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Optional;

public class UserQuotaService {
    private final ResourceQuotaPersistence resourceQuotaPersistence;
    private final UserQuotaPersistence userQuotaPersistence;
    private final HashMap<String, QuotaManager> quotaManagerCache = new HashMap<>();
    private final Clock clock;

    public UserQuotaService(ResourceQuotaPersistence resourceQuotaPersistence,
                            UserQuotaPersistence userQuotaPersistence,
                            Clock clock) {
        this.resourceQuotaPersistence = resourceQuotaPersistence;
        this.userQuotaPersistence = userQuotaPersistence;
        this.clock = clock;
    }

    public boolean tryAcquire(String username, String resourceId, int quantity) {
        UserQuotaId userQuotaId = UserQuotaId.idFor(username, resourceId);
        Optional<UserQuotaState> quotaState = userQuotaPersistence.findById(userQuotaId);

        QuotaManager quotaManager;
        if (quotaState.isPresent()) {
            UserQuotaState userQuotaState = quotaState.get();
            quotaManager = getQuotaManager(userQuotaId, userQuotaState.quotaManagerClassName(), userQuotaState.state());
        } else {
            ResourceQuota resourceQuota = resourceQuotaPersistence.findById(userQuotaId.resourceId()).orElseThrow();
            quotaManager = getQuotaManager(userQuotaId, resourceQuota.quotaManagerClassName(), resourceQuota.initialState());
        }

        return quotaManager.tryConsume(quantity);
    }

    private QuotaManager getQuotaManager(UserQuotaId userQuotaId, String className, Object state) {
        return quotaManagerCache.computeIfAbsent(className, s -> createQuotaManager(userQuotaId, className, state));
    }

    private QuotaManager createQuotaManager(UserQuotaId userQuotaId, String className, Object state) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(QuotaPersistence.class, state.getClass(), Clock.class);
            return (QuotaManager) constructor.newInstance(createFor(userQuotaId, className), state, clock);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public QuotaPersistence createFor(UserQuotaId userQuotaId, String className) {
        return state -> userQuotaPersistence.save(new UserQuotaState(userQuotaId, className, state));
    }
}
