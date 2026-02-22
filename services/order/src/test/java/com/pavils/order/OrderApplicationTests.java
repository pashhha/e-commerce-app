package com.pavils.order;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Stub test left from project scaffolding. @SpringBootTest cannot find @SpringBootConfiguration
// from package com.pavils.order (app is in com.pavils.ecommerce) and a full context load
// requires a live database, config-server, and Kafka broker.
// Comprehensive unit tests live in com.pavils.ecommerce.* instead.
@Disabled("Stub test from scaffolding — superseded by unit tests in com.pavils.ecommerce.*")
@SpringBootTest
class OrderApplicationTests {

    @Test
    void contextLoads() {
    }
}
