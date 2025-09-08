package com.userapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class UserApiApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // If this test passes, it means all the required beans and configurations are properly set up
    }

    @Test
    void applicationStarts() {
        // This test verifies that the application can start without any errors
        // It's a basic smoke test to ensure the application is properly configured
        // Removed the main() call to avoid port binding issues in test environment
    }
} 