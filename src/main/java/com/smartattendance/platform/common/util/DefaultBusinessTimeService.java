package com.smartattendance.platform.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DefaultBusinessTimeService implements BusinessTimeService {

    private final Clock clock;
    private final ZoneId businessZoneId;

    public DefaultBusinessTimeService(Clock clock, @Value("${app.business.timezone:UTC}") String timezone) {
        this.clock = clock;
        this.businessZoneId = ZoneId.of(timezone);
    }

    @Override
    public Instant getCurrentInstant() {
        return Instant.now(clock);
    }

    @Override
    public LocalDate getCurrentBusinessDate() {
        return LocalDate.now(clock.withZone(businessZoneId));
    }

    @Override
    public LocalDate getBusinessDate(Instant instant) {
        return instant.atZone(businessZoneId).toLocalDate();
    }

    @Override
    public ZoneId getBusinessZoneId() {
        return businessZoneId;
    }
}

