package com.myseotoolbox.quota4j;

import com.myseotoolbox.quota4j.persistence.ResourceQuotaPersistence;
import com.myseotoolbox.quota4j.model.ResourceQuota;
import com.myseotoolbox.quota4j.model.QuotaId;
import com.myseotoolbox.quota4j.model.QuotaState;
import com.myseotoolbox.quota4j.persistence.QuotaStatePersistence;
import com.myseotoolbox.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit;
import com.myseotoolbox.quota4j.quotamanager.quantityovertime.QuantityOverTimeQuotaManager;
import com.myseotoolbox.quota4j.quotamanager.quantityovertime.QuantityOverTimeState;
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

import static com.myseotoolbox.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit.limitOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuotaServiceTest {

    private static final String RESOURCE_ID = "crawler.maxCrawlsPerDay";
    private static final String OWNER_ID = "owner123@localhost";
    public static final QuantityOverTimeLimit TEN_PER_DAY_LIMIT = limitOf(10, Duration.ofDays(1));
    public static final QuantityOverTimeLimit ONE_PER_SECOND_LIMIT = limitOf(1, Duration.ofSeconds(1));
    public static final String RESOURCE_ID2 = "crawler.maxRecommendationsPerDay";


    TestClock testClock = new TestClock();

    private QuotaStatePersistence quotaStatePersistence = new TestQuotaStatePersistence();

    @Mock
    private ResourceQuotaPersistence resourceQuotaPersistence;

    QuotaService sut;


    @BeforeEach
    void setUp() {
        sut = new QuotaService(resourceQuotaPersistence, quotaStatePersistence);
        sut.registerQuotaManagerFactory(QuantityOverTimeQuotaManager.class.getName(), listener -> new QuantityOverTimeQuotaManager(listener, testClock));
    }


    @Test
    void shouldAllowAcquisitionOfAvailableQuota() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        assertTrue(sut.tryAcquire(OWNER_ID, RESOURCE_ID, 10));
    }

    @Test
    void shouldDeclineIfPreviouslyEmptied() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        sut.tryAcquire(OWNER_ID, RESOURCE_ID, 10);
        assertFalse(sut.tryAcquire(OWNER_ID, RESOURCE_ID, 1));
    }

    @Test
    void shouldDeclineWhenNotAvailableQuota() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        assertFalse(sut.tryAcquire(OWNER_ID, RESOURCE_ID, 10));
    }

    @Test
    void shouldReplenishWithTime() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        testClock.changeTime(cur -> cur.plus(1, ChronoUnit.DAYS));

        assertTrue(sut.tryAcquire(OWNER_ID, RESOURCE_ID, 10));
    }

    @Test
    void shouldReturnRemaining() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        sut.tryAcquire(OWNER_ID, RESOURCE_ID, 1);

        assertThat(((QuantityOverTimeState) sut.getQuotaState(OWNER_ID, RESOURCE_ID)).available(), is(4L));
    }

    @Test
    void shouldFailFastIfQuotaManagerIsNotRegistered() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManagerClassName(String.class.getName())
                .build();

        assertThrows(QuotaManagerNotRegisteredException.class, () -> sut.tryAcquire(OWNER_ID, RESOURCE_ID, 1));
    }

    @Test
    void shouldFailFastIfResourceQuotaDoesNotExist() {
        assertThrows(ResourceQuotaNotFoundException.class, () -> sut.tryAcquire(OWNER_ID, RESOURCE_ID, 1));
    }

    @Test
    void shouldRecoverStateFromQuotaStateIfExists() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH))
                .build();

        sut.tryAcquire(OWNER_ID, RESOURCE_ID, 10);
        assertFalse(sut.tryAcquire(OWNER_ID, RESOURCE_ID, 1));
    }

    @Test
    void shouldHandleStateForMultipleOwners() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        sut.tryAcquire("OWNER1", RESOURCE_ID, 10);
        sut.tryAcquire("OWNER2", RESOURCE_ID, 5);

        assertFalse(sut.tryAcquire("OWNER1", RESOURCE_ID, 1));
        assertTrue(sut.tryAcquire("OWNER2", RESOURCE_ID, 5));
    }

    @Test
    void ownerCanHaveMultipleResources() {
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH))
                .build();
        givenExistingResourceQuota()
                .forResourceId(RESOURCE_ID2)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingInitialState(new QuantityOverTimeState(ONE_PER_SECOND_LIMIT, 1, Instant.EPOCH))
                .build();


        assertTrue(sut.tryAcquire(OWNER_ID, RESOURCE_ID, 10));
        assertTrue(sut.tryAcquire(OWNER_ID, RESOURCE_ID2, 1));
        testClock.changeTime(instant -> instant.plusSeconds(1));
        assertTrue(sut.tryAcquire(OWNER_ID, RESOURCE_ID2, 1));
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

    private static class TestQuotaStatePersistence implements QuotaStatePersistence {


        private final Map<QuotaId, QuotaState> states = new HashMap<>();

        @Override
        public Optional<QuotaState> findById(QuotaId quotaId) {
            return Optional.ofNullable(states.get(quotaId));
        }

        @Override
        public QuotaState save(QuotaState quotaState) {
            return states.put(quotaState.id(), quotaState);
        }
    }

}