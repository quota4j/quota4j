package io.github.quota4j;

import io.github.quota4j.model.ResourceQuota;
import io.github.quota4j.model.UserQuotaId;
import io.github.quota4j.model.UserQuotaState;
import io.github.quota4j.persistence.QuotaPersistence;
import io.github.quota4j.persistence.ResourceQuotaPersistence;
import io.github.quota4j.persistence.UserQuotaPersistence;
import io.github.quota4j.quotamanager.QuotaManager;

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

    public boolean tryAcquire(String username, String resourceId, int quantity) throws InvalidQuotaManagerException {
        UserQuotaId userQuotaId = UserQuotaId.create(username, resourceId);
        Optional<UserQuotaState> possibleUserQuotaState = userQuotaPersistence.findById(userQuotaId);

        QuotaManager quotaManager = possibleUserQuotaState.map(userQuotaState -> {
            return getQuotaManager(userQuotaId, userQuotaState.quotaManagerClassName(), userQuotaState.state());
        }).orElseGet(() -> {
            ResourceQuota resourceQuota = resourceQuotaPersistence.findById(userQuotaId.resourceId()).orElseThrow();
            return getQuotaManager(userQuotaId, resourceQuota.quotaManagerClassName(), resourceQuota.initialState());
        });

        return quotaManager.tryConsume(quantity);
    }

    private QuotaManager getQuotaManager(UserQuotaId userQuotaId, String className, Object state) throws InvalidQuotaManagerException {
        return quotaManagerCache.computeIfAbsent(className, s -> createQuotaManager(userQuotaId, className, state));
    }

    private QuotaManager createQuotaManager(UserQuotaId userQuotaId, String className, Object state) throws InvalidQuotaManagerException {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(QuotaPersistence.class, state.getClass(), Clock.class);
            return (QuotaManager) constructor.newInstance(createFor(userQuotaId, className), state, clock);
        } catch (ClassCastException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new InvalidQuotaManagerException(e);
        }
    }

    public QuotaPersistence createFor(UserQuotaId userQuotaId, String className) {
        return state -> userQuotaPersistence.save(new UserQuotaStateImpl(userQuotaId, className, state));
    }

    private record UserQuotaStateImpl(UserQuotaId id, String quotaManagerClassName, Object state) implements UserQuotaState { }
}
