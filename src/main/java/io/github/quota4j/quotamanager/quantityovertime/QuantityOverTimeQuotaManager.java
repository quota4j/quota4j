package io.github.quota4j.quotamanager.quantityovertime;


import io.github.quota4j.quotamanager.QuotaManager;
import io.github.quota4j.persistence.QuotaPersistence;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


public class QuantityOverTimeQuotaManager implements QuotaManager {
    private final QuotaPersistence quotaPersistence;
    private final Clock clock;
    private QuantityOverTimeState currentState;

    public QuantityOverTimeQuotaManager(QuotaPersistence quotaPersistence, QuantityOverTimeState initialState, Clock clock) {
        this.quotaPersistence = quotaPersistence;
        this.clock = clock;
        this.currentState = initialState;
    }

    @Override
    public boolean tryConsume(long quantity) {
        Instant requestInstant = clock.instant();
        refill(requestInstant);
        if (currentState.remainingTokens() >= quantity) {
            updateState(currentState.remainingTokens() - quantity, requestInstant);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long getRemaining() {
        refill(clock.instant());
        return currentState.remainingTokens();
    }

    private void refill(Instant requestInstant) {
        Duration durationSinceLastRefill = Duration.between(currentState.lastRefill(), requestInstant);
        if (durationSinceLastRefill.compareTo(currentState.limit().duration()) >= 0) {
            updateState(currentState.limit().quantity(), requestInstant);
        }
    }

    private void updateState(long quantity, Instant requestInstant) {
        currentState = recreate(quantity, requestInstant);
        quotaPersistence.save(currentState);
    }

    private QuantityOverTimeState recreate(long quantity, Instant lastRefill) {
        return new QuantityOverTimeState(currentState.limit(), quantity, lastRefill);
    }

}
