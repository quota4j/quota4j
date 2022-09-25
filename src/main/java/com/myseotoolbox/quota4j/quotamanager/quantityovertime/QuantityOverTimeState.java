package com.myseotoolbox.quota4j.quotamanager.quantityovertime;

import java.io.Serializable;
import java.time.Instant;

public record QuantityOverTimeState(QuantityOverTimeLimit limit, long available, Instant lastRefill) implements Serializable {

}
