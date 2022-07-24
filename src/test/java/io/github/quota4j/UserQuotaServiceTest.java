package io.github.quota4j;

import io.github.quota4j.model.ResourceQuota;
import io.github.quota4j.model.UserQuotaId;
import io.github.quota4j.model.UserQuotaState;
import io.github.quota4j.persistence.ResourceQuotaPersistence;
import io.github.quota4j.persistence.UserQuotaPersistence;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeQuotaManager;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeState;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserQuotaServiceTest {

    private static final String RESOURCE_ID = "crawler.maxCrawlsPerDay";
    private static final String USERNAME = "user123@localhost";
    public static final QuantityOverTimeLimit TEN_PER_DAY_LIMIT = limitOf(10, Duration.ofDays(1));
    public static final QuantityOverTimeLimit ONE_PER_SECOND_LIMIT = limitOf(1, Duration.ofSeconds(1));


    TestClock testClock = new TestClock();

    private UserQuotaPersistence userQuotaPersistence = new TestUserQuotaPersistence();

    @Mock
    private ResourceQuotaPersistence resourceQuotaPersistence;

    UserQuotaService sut;


    @BeforeEach
    void setUp() {
        sut = new UserQuotaService(resourceQuotaPersistence, userQuotaPersistence);
        sut.registerQuotaManagerFactory(QuantityOverTimeQuotaManager.class.getName(), listener -> new QuantityOverTimeQuotaManager(listener, testClock));
    }

    @Test
    void shouldHandleStateForMultipleUsers() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        sut.tryAcquire("USER1", RESOURCE_ID, 10);
        sut.tryAcquire("USER2", RESOURCE_ID, 5);

        assertFalse(sut.tryAcquire("USER1", RESOURCE_ID, 1));
        assertTrue(sut.tryAcquire("USER2", RESOURCE_ID, 5));
    }

    @Test
    void usersCanHaveMultipleResources() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH))
                .build();
        givenExistingResourceQuota()
                .forResourceId("crawler.maxRecommendationsPerDay")
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(ONE_PER_SECOND_LIMIT, 1, Instant.EPOCH))
                .build();


        assertTrue(sut.tryAcquire(USERNAME, RESOURCE_ID, 10));
        assertTrue(sut.tryAcquire(USERNAME, "crawler.maxRecommendationsPerDay", 1));
        testClock.changeTime(instant -> instant.plusSeconds(1));
        assertTrue(sut.tryAcquire(USERNAME, "crawler.maxRecommendationsPerDay", 1));
    }

    @Test
    void shouldAllowAcquisitionOfAvailableQuota() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        assertTrue(sut.tryAcquire(USERNAME, RESOURCE_ID, 10));
    }

    @Test
    void shouldDeclineIfPreviouslyEmptied() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        sut.tryAcquire(USERNAME, RESOURCE_ID, 10);
        assertFalse(sut.tryAcquire(USERNAME, RESOURCE_ID, 1));
    }

    @Test
    void shouldDeclineWhenNotAvailableQuota() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        assertFalse(sut.tryAcquire(USERNAME, RESOURCE_ID, 10));
    }

    @Test
    void shouldReplenishWithTime() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        testClock.changeTime(cur -> cur.plus(1, ChronoUnit.DAYS));

        assertTrue(sut.tryAcquire(USERNAME, RESOURCE_ID, 10));
    }

    @Test
    void shouldFailFastIfQuotaManagerIsNotRegistered() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManagerClassName(String.class.getName())
                .build();

        assertThrows(QuotaManagerNotRegisteredException.class, () -> sut.tryAcquire(USERNAME, RESOURCE_ID, 1));
    }

    @Test
    void shouldRecoverStateFromUserQuotaIfExists() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH))
                .build();

        sut.tryAcquire(USERNAME, RESOURCE_ID, 10);
        assertFalse(sut.tryAcquire(USERNAME, RESOURCE_ID, 1));
    }

    private ResourceQuotaTestBuilder givenExistingResourceQuota() {
        return new ResourceQuotaTestBuilder(resourceQuotaPersistence);
    }

    private static class ResourceQuotaTestBuilder {
        private final ResourceQuotaPersistence resourceQuotaPersistenceMock;
        private String resourceId = RESOURCE_ID;
        private String quotaManagerClassName;
        private Object initialState = new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH);

        public ResourceQuotaTestBuilder(ResourceQuotaPersistence resourceQuotaPersistenceMock) {
            this.resourceQuotaPersistenceMock = resourceQuotaPersistenceMock;
        }

        public ResourceQuotaTestBuilder forResourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public ResourceQuotaTestBuilder withQuotaManager(Class<?> quotaManagerClass) {
            this.quotaManagerClassName = quotaManagerClass.getName();
            return this;
        }

        public ResourceQuotaTestBuilder havingInitialState(Object initialState) {
            this.initialState = initialState;
            return this;
        }

        public void build() {
            ResourceQuota resourceQuota = ResourceQuotaBuilder
                    .createWithResourceId(resourceId)
                    .withQuotaManager(quotaManagerClassName)
                    .initialState(initialState)
                    .build();
            when(resourceQuotaPersistenceMock.findById(resourceId)).thenReturn(Optional.of(resourceQuota));
        }

        public ResourceQuotaTestBuilder withQuotaManagerClassName(String className) {
            this.quotaManagerClassName = className;
            return this;
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