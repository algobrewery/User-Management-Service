package com.userapi.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.jdbc.Sql;

/**
 * Test configuration for PostgreSQL integration tests.
 * This configuration is active when the "test" profile is active.
 * 
 * Uses the same PostgreSQL database as production but with proper test isolation
 * through transactions and @Sql annotations.
 */
@TestConfiguration
@Profile("test")
public class PostgreSQLTestConfig {
    
    // No additional beans needed - using application-test.properties for configuration
    // Test isolation is handled through @Transactional and @Sql annotations
}
