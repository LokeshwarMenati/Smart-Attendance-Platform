package com.smartattendance.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class SmartAttendanceApplicationTests {

    @Test
    @DisplayName("Application context loads successfully with safe test configurations")
    void contextLoads() {
        assertDoesNotThrow(() -> {});
    }
}

