package com.myseotoolbox.quota4j.quotamanager.quantityovertime;


import com.myseotoolbox.quota4j.persistence.QuotaManagerStateChangeListener;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

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
        if (currentState.available() >= quantity) {
            updateState(currentState, currentState.available() - quantity, currentState.lastRefill());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public QuantityOverTimeState getCurrentState(QuantityOverTimeState currentState) {
        Instant requestInstant = clock.instant();
        return refill(currentState, requestInstant);
    }

    private QuantityOverTimeState refill(QuantityOverTimeState currentState, Instant requestInstant) {
        Duration durationSinceLastRefill = Duration.between(currentState.lastRefill(), requestInstant);
        if (durationSinceLastRefill.compareTo(currentState.limit().duration()) >= 0) {
            return updateState(currentState, Math.max(currentState.limit().quantity(), currentState.available()), requestInstant);
        }
        return currentState;
    }

    private QuantityOverTimeState updateState(QuantityOverTimeState currentState, long quantity, Instant lastRefill) {
        QuantityOverTimeState newState = new QuantityOverTimeState(currentState.limit(), quantity, lastRefill);
        stateChangeListener.stateChanged(newState);
        return newState;
    }


}
