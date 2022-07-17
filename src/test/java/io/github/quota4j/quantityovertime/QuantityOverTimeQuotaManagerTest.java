package io.github.quota4j.quantityovertime;


import io.github.quota4j.TestClock;
import io.github.quota4j.persistence.QuotaManagerPersistence;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeQuotaManager;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeState;
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

    private QuotaManagerPersistence quotaManagerPersistence = Mockito.spy(new QuotaManagerPersistence() {
        @Override
        public void save(Object newState) {
            state = (QuantityOverTimeState) newState;
        }
    });

    QuantityOverTimeQuotaManager sut;

    @BeforeEach
    void setUp() {
        sut = new QuantityOverTimeQuotaManager(quotaManagerPersistence, testClock);
    }

    @Test
    void shouldAllowToConsumeTokens() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        assertTrue(sut.tryConsume(state, 1));
    }

    @Test
    public void shouldNotAllowToExceedQuota() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        assertFalse(sut.tryConsume(state, 11));
    }

    @Test
    public void shouldNotAllowToExceedQuotaWithSubsequentRequests() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        assertTrue(sut.tryConsume(state, 10));
        assertFalse(sut.tryConsume(state, 1));
    }

    @Test
    public void shouldReplenishQuotaWhenTimeExpires() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(state, 10);
        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.DAYS));
        assertTrue(sut.tryConsume(state, 10));
    }

    @Test
    void shouldNotReplenishEarly() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(state, 10);
        Instant enoughTime = testClock.plus(1, ChronoUnit.DAYS);
        Instant notEnoughTime = enoughTime.minusMillis(1);
        testClock.changeTime(curTime -> notEnoughTime);
        assertFalse(sut.tryConsume(state, 1));
    }

    @Test
    public void shouldProvideRemainingQuantity() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(state, 5);
        assertThat(sut.getRemaining(state), is(5L));
    }

    @Test
    public void shouldReplenishAvailableWithoutWrite() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(state, 5);
        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.DAYS));
        assertThat(sut.getRemaining(state), is(10L));
    }

    @Test
    public void quotaIsNotCumulative() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        assertThat(sut.getRemaining(state), is(10L));
        testClock.changeTime(curTime -> curTime.plus(10, ChronoUnit.DAYS));
        assertThat(sut.getRemaining(state), is(10L));
    }

    @Test
    void shouldReadRemainingTokensFromPassedState() {
        givenQuantityOverTimeState()
                .withLimit(TEN_PER_DAY_LIMIT)
                .withRemainingTokens(2)
                .build();

        assertThat(sut.getRemaining(state), is(2L));
    }

    @Test
    void shouldReadLastRefillFromPassedState() {
        givenQuantityOverTimeState()
                .withLimit(TEN_PER_DAY_LIMIT)
                .withRemainingTokens(2)
                .withLastRefill(testClock.instant().minus(12, ChronoUnit.HOURS))
                .build();

        assertThat(sut.getRemaining(state), is(2L));
        testClock.changeTime(curTime -> curTime.plus(12, ChronoUnit.HOURS));
        assertThat(sut.getRemaining(state), is(10L));
    }

    @Test
    void shouldNotPersistIfNoStateChange() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.getRemaining(state);
        verify(quotaManagerPersistence, never()).save(any());
    }

    @Test
    void shouldUpdateRemainingWhenStateChanges() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();

        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.HOURS));
        sut.tryConsume(state, 1);

        verify(quotaManagerPersistence, times(1)).save(
                new QuantityOverTimeState(TEN_PER_DAY_LIMIT, TEN_PER_DAY_LIMIT.quantity() - 1, any())
        );
    }

    @Test
    void lastRefillIsNotUpdatedIfNoRefillOccur() {
        givenQuantityOverTimeState().withLimit(TEN_PER_DAY_LIMIT).build();

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
        private long remainingTokens;
        private Instant lastRefillInstant = testClock.instant();

        public QuantityOverTimeStateBuilder withLimit(QuantityOverTimeLimit limit) {
            this.limit = limit;
            this.remainingTokens = limit.quantity();
            return this;
        }

        public QuantityOverTimeStateBuilder withRemainingTokens(long remainingTokens) {
            this.remainingTokens = remainingTokens;
            return this;
        }

        public QuantityOverTimeStateBuilder withLastRefill(Instant lastRefillInstant) {
            this.lastRefillInstant = lastRefillInstant;
            return this;
        }

        public void build() {
            state = new QuantityOverTimeState(limit, remainingTokens, lastRefillInstant);
        }
    }

}