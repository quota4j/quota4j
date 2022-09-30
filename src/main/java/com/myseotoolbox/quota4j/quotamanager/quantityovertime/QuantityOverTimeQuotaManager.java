package com.myseotoolbox.quota4j.quotamanager.quantityovertime;


import com.myseotoolbox.quota4j.quotamanager.AcquireResponse;
import com.myseotoolbox.quota4j.quotamanager.QuotaManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


public class QuantityOverTimeQuotaManager implements QuotaManager<QuantityOverTimeState> {
    private final Clock clock;

    public QuantityOverTimeQuotaManager(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AcquireResponse<QuantityOverTimeState> tryAcquire(QuantityOverTimeState state, long quantity) {
        QuantityOverTimeState currentState = getCurrentState(state);
        if (currentState.available() >= quantity) {
            QuantityOverTimeState newState = updateState(currentState, currentState.available() - quantity, currentState.lastRefill());
            return AcquireResponse.grantedWithState(newState);
        } else {
            return AcquireResponse.declinedWithState(currentState);
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
        return new QuantityOverTimeState(currentState.limit(), quantity, lastRefill);
    }


}
