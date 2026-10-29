package com.smartattendance.platform.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Ensures JVM-wide UTC timezone setting for consistent database and date-time handling.
 */
@Configuration
public class TimezoneConfig {

    private static final Logger log = LoggerFactory.getLogger(TimezoneConfig.class);

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("Default JVM TimeZone explicitly set to UTC");
    }
}

