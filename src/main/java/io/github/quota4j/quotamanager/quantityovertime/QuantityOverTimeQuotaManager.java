package io.github.quota4j.quotamanager.quantityovertime;


import io.github.quota4j.quotamanager.QuotaManager;
import io.github.quota4j.persistence.QuotaManagerPersistence;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


public class QuantityOverTimeQuotaManager implements QuotaManager<QuantityOverTimeState> {
    private final QuotaManagerPersistence quotaManagerPersistence;
    private final Clock clock;

    public QuantityOverTimeQuotaManager(QuotaManagerPersistence quotaManagerPersistence, Clock clock) {
        this.quotaManagerPersistence = quotaManagerPersistence;
        this.clock = clock;
    }

    @Override
    public boolean tryConsume(QuantityOverTimeState currentState, long quantity) {
        Instant requestInstant = clock.instant();
        currentState = refill(currentState, requestInstant);
        if (currentState.remainingTokens() >= quantity) {
            updateState(currentState, currentState.remainingTokens() - quantity, currentState.lastRefill());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long getRemaining(QuantityOverTimeState currentState) {
        currentState = refill(currentState, clock.instant());
        return currentState.remainingTokens();
    }

    private QuantityOverTimeState refill(QuantityOverTimeState currentState, Instant requestInstant) {
        Duration durationSinceLastRefill = Duration.between(currentState.lastRefill(), requestInstant);
        if (durationSinceLastRefill.compareTo(currentState.limit().duration()) >= 0) {
            return updateState(currentState, currentState.limit().quantity(), requestInstant);
        }
        return currentState;
    }

    private QuantityOverTimeState updateState(QuantityOverTimeState currentState, long quantity, Instant lastRefill) {
        QuantityOverTimeState newState = new QuantityOverTimeState(currentState.limit(), quantity, lastRefill);
        quotaManagerPersistence.save(newState);
        return newState;
    }


}
