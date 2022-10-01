package com.myseotoolbox.quota4j;

import com.myseotoolbox.quota4j.model.QuotaStateId;
import com.myseotoolbox.quota4j.model.QuotaState;
import com.myseotoolbox.quota4j.model.Quota;
import com.myseotoolbox.quota4j.persistence.QuotaStatePersistence;
import com.myseotoolbox.quota4j.persistence.QuotaPersistence;
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

    private static final String QUOTA_ID = "crawler.maxCrawlsPerDay";
    private static final String OWNER_ID = "owner123@localhost";
    public static final QuantityOverTimeLimit TEN_PER_DAY_LIMIT = limitOf(10, Duration.ofDays(1));
    public static final QuantityOverTimeLimit ONE_PER_SECOND_LIMIT = limitOf(1, Duration.ofSeconds(1));
    public static final String QUOTA_ID2 = "crawler.maxRecommendationsPerDay";


    TestClock testClock = new TestClock();

    private QuotaStatePersistence quotaStatePersistence = new TestQuotaStatePersistence();

    @Mock
    private QuotaPersistence quotaPersistence;

    QuotaService sut;


    @BeforeEach
    void setUp() {
        sut = new QuotaService(quotaPersistence, quotaStatePersistence);
        sut.registerQuotaManagerFactory(QuantityOverTimeQuotaManager.class.getName(), () -> new QuantityOverTimeQuotaManager(testClock));
    }


    @Test
    void shouldAllowAcquisitionOfAvailableQuota() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        assertTrue(sut.tryAcquire(OWNER_ID, QUOTA_ID, 10).result());
    }

    @Test
    void shouldDeclineIfPreviouslyEmptied() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        sut.tryAcquire(OWNER_ID, QUOTA_ID, 10);
        assertFalse(sut.tryAcquire(OWNER_ID, QUOTA_ID, 1).result());
    }

    @Test
    void shouldDeclineWhenNotAvailableQuota() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        assertFalse(sut.tryAcquire(OWNER_ID, QUOTA_ID, 10).result());
    }

    @Test
    void shouldReplenishWithTime() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        testClock.changeTime(cur -> cur.plus(1, ChronoUnit.DAYS));

        assertTrue(sut.tryAcquire(OWNER_ID, QUOTA_ID, 10).result());
    }

    @Test
    void shouldReturnRemaining() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        sut.tryAcquire(OWNER_ID, QUOTA_ID, 1);

        assertThat(((QuantityOverTimeState) sut.getQuotaState(OWNER_ID, QUOTA_ID)).available(), is(4L));
    }

    @Test
    void shouldPersistAfterAcquire() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        sut.tryAcquire(OWNER_ID, QUOTA_ID, 1);

        assertThat(((QuantityOverTimeState) quotaStatePersistence.findById(QuotaStateId.create(OWNER_ID, QUOTA_ID)).get().currentState()).available(), is(4L));
    }

    @Test
    void gettingCurrentStatePersistsChanges() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 5, Instant.EPOCH))
                .build();

        testClock.changeTime(cur -> cur.plus(1, ChronoUnit.DAYS));
        sut.getQuotaState(OWNER_ID, QUOTA_ID);
        assertThat(((QuantityOverTimeState) quotaStatePersistence.findById(QuotaStateId.create(OWNER_ID, QUOTA_ID)).get().currentState()).available(), is(10L));
    }

    @Test
    void shouldFailFastIfQuotaManagerIsNotRegistered() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManagerClassName(String.class.getName())
                .build();

        assertThrows(QuotaManagerNotRegisteredException.class, () -> sut.tryAcquire(OWNER_ID, QUOTA_ID, 1));
    }

    @Test
    void shouldFailFastIfQuotaDoesNotExist() {
        assertThrows(QuotaNotFoundException.class, () -> sut.tryAcquire(OWNER_ID, QUOTA_ID, 1));
    }

    @Test
    void shouldRecoverStateFromQuotaStateIfExists() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH))
                .build();

        sut.tryAcquire(OWNER_ID, QUOTA_ID, 10);
        assertFalse(sut.tryAcquire(OWNER_ID, QUOTA_ID, 1).result());
    }

    @Test
    void shouldHandleStateForMultipleOwners() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .build();

        sut.tryAcquire("OWNER1", QUOTA_ID, 10);
        sut.tryAcquire("OWNER2", QUOTA_ID, 5);

        assertFalse(sut.tryAcquire("OWNER1", QUOTA_ID, 1).result());
        assertTrue(sut.tryAcquire("OWNER2", QUOTA_ID, 5).result());
    }

    @Test
    void ownerCanHaveMultipleQuota() {
        givenExistingQuota()
                .forQuotaId(QUOTA_ID)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH))
                .build();
        givenExistingQuota()
                .forQuotaId(QUOTA_ID2)
                .withQuotaManager(QuantityOverTimeQuotaManager.class)
                .havingDefaultState(new QuantityOverTimeState(ONE_PER_SECOND_LIMIT, 1, Instant.EPOCH))
                .build();


        assertTrue(sut.tryAcquire(OWNER_ID, QUOTA_ID, 10).result());
        assertTrue(sut.tryAcquire(OWNER_ID, QUOTA_ID2, 1).result());
        testClock.changeTime(instant -> instant.plusSeconds(1));
        assertTrue(sut.tryAcquire(OWNER_ID, QUOTA_ID2, 1).result());
    }

    private QuotaTestBuilder givenExistingQuota() {
        return new QuotaTestBuilder(quotaPersistence);
    }

    private static class QuotaTestBuilder {
        private final QuotaPersistence quotaPersistenceMock;
        private String quotaId = QUOTA_ID;
        private String quotaManagerClassName;
        private Object defaultState = new QuantityOverTimeState(TEN_PER_DAY_LIMIT, 10, Instant.EPOCH);

        public QuotaTestBuilder(QuotaPersistence quotaPersistenceMock) {
            this.quotaPersistenceMock = quotaPersistenceMock;
        }

        public QuotaTestBuilder forQuotaId(String quotaId) {
            this.quotaId = quotaId;
            return this;
        }

        public QuotaTestBuilder withQuotaManager(Class<?> quotaManagerClass) {
            this.quotaManagerClassName = quotaManagerClass.getName();
            return this;
        }

        public QuotaTestBuilder havingDefaultState(Object defaultState) {
            this.defaultState = defaultState;
            return this;
        }

        public void build() {
            Quota quota = QuotaBuilder
                    .createWithQuotaId(quotaId)
                    .withQuotaManager(quotaManagerClassName)
                    .defaultState(defaultState)
                    .build();
            when(quotaPersistenceMock.findById(quotaId)).thenReturn(Optional.of(quota));
        }

        public QuotaTestBuilder withQuotaManagerClassName(String className) {
            this.quotaManagerClassName = className;
            return this;
        }
    }

    private static class TestQuotaStatePersistence implements QuotaStatePersistence {


        private final Map<QuotaStateId, QuotaState> states = new HashMap<>();

        @Override
        public Optional<QuotaState> findById(QuotaStateId quotaStateId) {
            return Optional.ofNullable(states.get(quotaStateId));
        }

        @Override
        public QuotaState save(QuotaState quotaState) {
            return states.put(quotaState.id(), quotaState);
        }
    }

}