package com.myseotoolbox.quota4j;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.function.Function;

public class TestClock extends Clock {

    private Instant currentTime = Instant.EPOCH;

    public void changeTime(Function<Instant, Instant> timeModifier) {
        this.currentTime = timeModifier.apply(this.currentTime);
    }

    @Override
    public Instant instant() {
        return this.currentTime;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zoneId) {
        throw new UnsupportedOperationException("This is a test clock");
    }

    public Instant plus(long amountToAdd, TemporalUnit temporalUnit) {
        return currentTime.plus(amountToAdd, temporalUnit);
    }

    public Instant plusSeconds(long secondsToAdd) {
        return currentTime.plusSeconds(secondsToAdd);
    }
}