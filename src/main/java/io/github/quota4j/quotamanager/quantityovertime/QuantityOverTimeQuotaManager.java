package io.github.quota4j.quotamanager.quantityovertime;


import io.github.quota4j.persistence.QuotaManagerStateChangeListener;
import io.github.quota4j.quotamanager.QuotaManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


public class QuantityOverTimeQuotaManager implements QuotaManager<QuantityOverTimeState> {
    private final QuotaManagerStateChangeListener stateChangeListener;
    private final Clock clock;

    public QuantityOverTimeQuotaManager(QuotaManagerStateChangeListener listener, Clock clock) {
        this.stateChangeListener = listener;
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
        stateChangeListener.stateChanged(newState);
        return newState;
    }


}
