package com.vidal.idempotency.service;

import com.vidal.idempotency.config.IdempotencyAutoConfiguration;
import com.vidal.idempotency.idempotency.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class IdempotencyServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class))
            .withPropertyValues(
                    "spring.data.redis.host=" + redis.getHost(),
                    "spring.data.redis.port=" + redis.getFirstMappedPort()
            );

    @Test
    void shouldConfigureWithExternalRedis() {
        contextRunner.run(context -> {
            IdempotencyService service = context.getBean(IdempotencyService.class);
            assertThat(service).isNotNull();
        });
    }
}