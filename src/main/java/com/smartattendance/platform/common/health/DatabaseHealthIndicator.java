package com.smartattendance.platform.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Dedicated database connectivity health check verified through HikariCP.
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            String dbProduct = connection.getMetaData().getDatabaseProductName();
            return Health.up()
                    .withDetail("database", dbProduct)
                    .withDetail("connectivity", "Successful")
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("connectivity", "Failed: " + ex.getMessage())
                    .build();
        }
    }
}

