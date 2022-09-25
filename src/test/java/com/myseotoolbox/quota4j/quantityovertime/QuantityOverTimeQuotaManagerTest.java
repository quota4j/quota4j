package com.myseotoolbox.quota4j.quantityovertime;


import com.myseotoolbox.quota4j.TestClock;
import com.myseotoolbox.quota4j.persistence.QuotaManagerStateChangeListener;
import com.myseotoolbox.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit;
import com.myseotoolbox.quota4j.quotamanager.quantityovertime.QuantityOverTimeQuotaManager;
import com.myseotoolbox.quota4j.quotamanager.quantityovertime.QuantityOverTimeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuantityOverTimeQuotaManagerTest {

    private static final QuantityOverTimeLimit TEN_PER_DAY_LIMIT = new QuantityOverTimeLimit(10, Duration.ofDays(1));
    private TestClock testClock = new TestClock();

    QuantityOverTimeState state;

    private QuotaManagerStateChangeListener quotaManagerStateChangeListener = Mockito.spy(new QuotaManagerStateChangeListener() {
        @Override
        public void stateChanged(Object newState) {
            state = (QuantityOverTimeState) newState;
        }
    });

    QuantityOverTimeQuotaManager sut;

    @BeforeEach
    void setUp() {
        sut = new QuantityOverTimeQuotaManager(quotaManagerStateChangeListener, testClock);
    }

    @Test
    void shouldAllowToConsumeTokens() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        assertTrue(sut.tryConsume(state, 1));
    }

    @Test
    public void shouldNotAllowToExceedQuota() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        assertFalse(sut.tryConsume(state, 11));
    }

    @Test
    public void shouldNotAllowToExceedQuotaWithSubsequentRequests() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        assertTrue(sut.tryConsume(state, 10));
        assertFalse(sut.tryConsume(state, 1));
    }

    @Test
    public void shouldReplenishQuotaWhenTimeExpires() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        sut.tryConsume(state, 10);
        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.DAYS));
        assertTrue(sut.tryConsume(state, 10));
    }

    @Test
    void shouldNotReplenishEarly() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        sut.tryConsume(state, 10);
        Instant enoughTime = testClock.plus(1, ChronoUnit.DAYS);
        Instant notEnoughTime = enoughTime.minusMillis(1);
        testClock.changeTime(curTime -> notEnoughTime);
        assertFalse(sut.tryConsume(state, 1));
    }

    @Test
    void resourceQuotaDefaultAvailableShouldNotBeOverride() {
        // If I have a limit of 1/day and an initial available of 10, I want the user to be able to use the 10 available,
        // even in 1 second and then start imposing limit.

        givenQuantityOverTimeState()
                .withLimit(TEN_PER_DAY_LIMIT)
                .withAvailable(100)
                .init();

        sut.tryConsume(state, 10);
        testClock.changeTime(instant -> instant.plus(2, ChronoUnit.DAYS));
        assertThat(sut.getCurrentState(state).available(), is(90L));
    }

    @Test
    public void shouldProvideCurrentState() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        sut.tryConsume(state, 5);
        assertThat(sut.getCurrentState(state).available(), is(5L));
        assertThat(sut.getCurrentState(state).lastRefill(), is(Instant.EPOCH));
        assertThat(sut.getCurrentState(state).limit(), is(TEN_PER_DAY_LIMIT));
    }

    @Test
    public void shouldReplenishAvailableWithoutWrite() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        sut.tryConsume(state, 5);
        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.DAYS));
        assertThat(sut.getCurrentState(state).available(), is(10L));
    }

    @Test
    public void quotaIsNotCumulative() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        assertThat(sut.getCurrentState(state).available(), is(10L));
        testClock.changeTime(curTime -> curTime.plus(10, ChronoUnit.DAYS));
        assertThat(sut.getCurrentState(state).available(), is(10L));
    }

    @Test
    void shouldReadRemainingTokensFromPassedState() {
        givenQuantityOverTimeState()
                .withLimit(TEN_PER_DAY_LIMIT)
                .withAvailable(2)
                .init();

        assertThat(sut.getCurrentState(state).available(), is(2L));
    }

    @Test
    void shouldReadLastRefillFromPassedState() {
        givenQuantityOverTimeState()
                .withLimit(TEN_PER_DAY_LIMIT)
                .withAvailable(2)
                .withLastRefill(testClock.instant().minus(12, ChronoUnit.HOURS))
                .init();

        assertThat(sut.getCurrentState(state).available(), is(2L));
        testClock.changeTime(curTime -> curTime.plus(12, ChronoUnit.HOURS));
        assertThat(sut.getCurrentState(state).available(), is(10L));
    }

    @Test
    void shouldNotPersistIfNoStateChange() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();
        sut.getCurrentState(state).available();
        verify(quotaManagerStateChangeListener, never()).stateChanged(any());
    }

    @Test
    void shouldUpdateRemainingWhenStateChanges() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();

        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.HOURS));
        sut.tryConsume(state, 1);

        verify(quotaManagerStateChangeListener, times(1)).stateChanged(
                new QuantityOverTimeState(TEN_PER_DAY_LIMIT, TEN_PER_DAY_LIMIT.quantity() - 1, any())
        );
    }

    @Test
    void lastRefillIsNotUpdatedIfNoRefillOccur() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).init();

        testClock.changeTime(curTime -> curTime.plus(12, ChronoUnit.HOURS));
        // if last refill is updated with this time, while updating the remaining, then the next request will think only
        // the last 12 hours have passed since the last refill
        sut.tryConsume(state, 10);
        testClock.changeTime(curTime -> curTime.plus(12, ChronoUnit.HOURS));

        assertTrue(sut.tryConsume(state, 10));
    }

    private QuantityOverTimeStateBuilder givenQuantityOverTimeState() {
        return new QuantityOverTimeStateBuilder();
    }

    private class QuantityOverTimeStateBuilder {
        private QuantityOverTimeLimit limit;
        private long available;
        private Instant lastRefillInstant = testClock.instant();

        public QuantityOverTimeStateBuilder withLimit(QuantityOverTimeLimit limit) {
            this.limit = limit;
            this.available = limit.quantity();
            return this;
        }

        public QuantityOverTimeStateBuilder withAvailable(long available) {
            this.available = available;
            return this;
        }

        public QuantityOverTimeStateBuilder withLastRefill(Instant lastRefillInstant) {
            this.lastRefillInstant = lastRefillInstant;
            return this;
        }

        public void init() {
            state = new QuantityOverTimeState(limit, available, lastRefillInstant);
        }
    }

}