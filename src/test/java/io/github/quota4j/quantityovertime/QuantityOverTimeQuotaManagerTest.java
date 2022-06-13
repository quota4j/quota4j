package io.github.quota4j.quantityovertime;


import io.github.quota4j.TestClock;
import io.github.quota4j.persistence.QuotaPersistence;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeLimit;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeQuotaManager;
import io.github.quota4j.quotamanager.quantityovertime.QuantityOverTimeState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
    public static final String TEST_BUCKET_KEY = "crawler.maxCrawlCount";
    private TestClock testClock = new TestClock();

    @Mock
    private QuotaPersistence quotaPersistence;

    QuantityOverTimeQuotaManager sut;

    @Test
    void shouldAllowToConsumeTokens() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        assertTrue(sut.tryConsume(1));
    }

    @Test
    public void shouldNotAllowToExceedQuota() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        assertFalse(sut.tryConsume(11));
    }

    @Test
    public void shouldNotAllowToExceedQuotaWithSubsequentRequests() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        assertTrue(sut.tryConsume(10));
        assertFalse(sut.tryConsume(1));
    }

    @Test
    public void shouldReplenishQuotaWhenTimeExpires() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(10);
        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.DAYS));
        assertTrue(sut.tryConsume(10));
    }

    @Test
    void shouldNotReplenishEarly() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(10);
        Instant enoughTime = testClock.plus(1, ChronoUnit.DAYS);
        Instant notEnoughTime = enoughTime.minusMillis(1);
        testClock.changeTime(curTime -> notEnoughTime);
        assertFalse(sut.tryConsume(1));
    }

    @Test
    public void shouldProvideRemainingQuantity() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(5);
        assertThat(sut.getRemaining(), is(5L));
    }

    @Test
    public void shouldReplenishAvailableWithoutWrite() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.tryConsume(5);
        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.DAYS));
        assertThat(sut.getRemaining(), is(10L));
    }

    @Test
    public void quotaIsNotCumulative() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        assertThat(sut.getRemaining(), is(10L));
        testClock.changeTime(curTime -> curTime.plus(10, ChronoUnit.DAYS));
        assertThat(sut.getRemaining(), is(10L));
    }

    @Test
    void shouldReadRemainingTokensFromPassedState() {
        sut = givenTokenBucket()
                .withLimit(TEN_PER_DAY_LIMIT)
                .withRemainingTokens(2)
                .build();

        assertThat(sut.getRemaining(), is(2L));
        testClock.changeTime(curTime -> curTime.plus(1, ChronoUnit.DAYS));
        assertThat(sut.getRemaining(), is(10L));
    }

    @Test
    void shouldReadLastRefillFromPassedState() {
        sut = givenTokenBucket()
                .withLimit(TEN_PER_DAY_LIMIT)
                .withRemainingTokens(2)
                .withLastRefill(testClock.instant().minus(12, ChronoUnit.HOURS))
                .build();

        assertThat(sut.getRemaining(), is(2L));
        testClock.changeTime(curTime -> curTime.plus(12, ChronoUnit.HOURS));
        assertThat(sut.getRemaining(), is(10L));
    }

    @Test
    void shouldNotPersistIfNoStateChange() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();
        sut.getRemaining();
        verify(quotaPersistence, never()).save(any());
    }

    @Test
    void shouldUpdateWhenStateChanges() {
        sut = givenTokenBucket().withLimit(TEN_PER_DAY_LIMIT).build();

        testClock.changeTime(curTime -> curTime.plus(30, ChronoUnit.SECONDS));
        sut.tryConsume(1);

        verify(quotaPersistence, times(1)).save(
                new QuantityOverTimeState(TEN_PER_DAY_LIMIT, TEN_PER_DAY_LIMIT.quantity() - 1, testClock.instant())
        );
    }

    private QuantityOverTimeQuotaManagerBuilder givenTokenBucket() {
        return new QuantityOverTimeQuotaManagerBuilder();
    }

    private class QuantityOverTimeQuotaManagerBuilder {
        private QuantityOverTimeLimit limit;
        private long remainingTokens;
        private Instant lastRefillInstant = testClock.instant();

        public QuantityOverTimeQuotaManagerBuilder withLimit(QuantityOverTimeLimit limit) {
            this.limit = limit;
            this.remainingTokens = limit.quantity();
            return this;
        }

        public QuantityOverTimeQuotaManagerBuilder withRemainingTokens(long remainingTokens) {
            this.remainingTokens = remainingTokens;
            return this;
        }

        public QuantityOverTimeQuotaManagerBuilder withLastRefill(Instant lastRefillInstant) {
            this.lastRefillInstant = lastRefillInstant;
            return this;
        }

        public QuantityOverTimeQuotaManager build() {
            return new QuantityOverTimeQuotaManager(quotaPersistence, new QuantityOverTimeState(limit, remainingTokens, lastRefillInstant), testClock);
        }
    }

}