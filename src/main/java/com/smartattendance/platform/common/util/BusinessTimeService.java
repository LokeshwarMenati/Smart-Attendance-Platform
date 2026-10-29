package com.smartattendance.platform.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public interface BusinessTimeService {
    Instant getCurrentInstant();
    LocalDate getCurrentBusinessDate();
    LocalDate getBusinessDate(Instant instant);
    ZoneId getBusinessZoneId();
}
