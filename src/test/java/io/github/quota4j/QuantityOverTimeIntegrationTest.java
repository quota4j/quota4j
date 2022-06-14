package io.github.quota4j;

import io.github.quota4j.model.ResourceQuota;
import io.github.quota4j.model.UserQuotaId;
import io.github.quota4j.model.UserQuotaState;
import io.github.quota4j.persistence.ResourceQuotaPersistence;
import io.github.quota4j.persistence.UserQuotaPersistence;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeQuotaManager;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeState;
import io.github.quota4j.utils.ResourceQuotaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit.limitOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuantityOverTimeIntegrationTest {

    private static final String RESOURCE_ID = "crawler.maxCrawlsPerDay";
    private static final String USERNAME = "user123@localhost";
    public static final QuantityOverTimeLimit TEN_PER_DAY_LIMIT = limitOf(10, Duration.ofDays(1));

    TestClock testClock = new TestClock();

    private UserQuotaPersistence userQuotaPersistence = new TestUserQuotaPersistence();

    @Mock
    private ResourceQuotaPersistence resourceQuotaPersistence;

    UserQuotaService sut;


    @BeforeEach
    void setUp() {
        sut = new UserQuotaService(resourceQuotaPersistence, userQuotaPersistence, testClock);
    }


    @Test
    void shouldAllowAcquisitionOfAvailableQuota() {
        givenExistingResourceQuota()
                .withResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .initialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 3, Instant.EPOCH))
                .build();

        assertTrue(sut.tryAcquire(USERNAME, RESOURCE_ID, 3));
    }

    @Test
    void shouldDeclineWhenNotAvailableQuota() {
        givenExistingResourceQuota()
                .withResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .initialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        assertFalse(sut.tryAcquire(USERNAME, RESOURCE_ID, 10));
    }

    @Test
    void shouldReplenishWithTime() {
        givenExistingResourceQuota()
                .withResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .initialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        testClock.changeTime(cur -> cur.plus(1, ChronoUnit.DAYS));

        assertTrue(sut.tryAcquire(USERNAME, RESOURCE_ID, 10));
    }

    @Test
    void shouldRecoverStateFromUserQuotaIfExists() {
        givenExistingResourceQuota()
                .withResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .initialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 3, Instant.EPOCH))
                .build();

        sut.tryAcquire(USERNAME, RESOURCE_ID, 3);

        sut = new UserQuotaService(resourceQuotaPersistence, userQuotaPersistence, testClock);

        assertFalse(sut.tryAcquire(USERNAME, RESOURCE_ID, 3));
    }

    private ResourceQuotaTestBuilder givenExistingResourceQuota() {
        return new ResourceQuotaTestBuilder(resourceQuotaPersistence);
    }

    private static class ResourceQuotaTestBuilder {
        private final ResourceQuotaPersistence resourceQuotaPersistenceMock;
        private String resourceId;
        private Class quotaManagerClass;
        private Object initialState;

        public ResourceQuotaTestBuilder(ResourceQuotaPersistence resourceQuotaPersistenceMock) {
            this.resourceQuotaPersistenceMock = resourceQuotaPersistenceMock;
        }

        public ResourceQuotaTestBuilder withResourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public ResourceQuotaTestBuilder withQuotaManager(Class<?> quotaManagerClass) {
            this.quotaManagerClass = quotaManagerClass;
            return this;
        }

        public ResourceQuotaTestBuilder initialState(Object initialState) {
            this.initialState = initialState;
            return this;
        }

        public void build() {
            ResourceQuota resourceQuota = ResourceQuotaBuilder
                    .createWithResourceId(resourceId)
                    .withQuotaManager(quotaManagerClass)
                    .initialState(initialState)
                    .build();
            when(resourceQuotaPersistenceMock.findById(resourceId)).thenReturn(Optional.of(resourceQuota));
        }
    }

    private static class TestUserQuotaPersistence implements UserQuotaPersistence {


        private final Map<UserQuotaId, UserQuotaState> states = new HashMap<>();

        @Override
        public Optional<UserQuotaState> findById(UserQuotaId userQuotaId) {
            return Optional.ofNullable(states.get(userQuotaId));
        }

        @Override
        public UserQuotaState save(UserQuotaState userQuotaState) {
            return states.put(userQuotaState.id(), userQuotaState);
        }
    }

}