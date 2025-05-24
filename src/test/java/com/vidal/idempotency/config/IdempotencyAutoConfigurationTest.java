package com.vidal.idempotency.config;

import com.vidal.idempotency.idempotency.aspect.IdempotencyAspect;
import com.vidal.idempotency.idempotency.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class));

    @Test
    void shouldRegisterBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IdempotencyService.class);
            assertThat(context).hasSingleBean(IdempotencyAspect.class);
            assertThat(context).hasSingleBean(RedisTemplate.class);
        });
    }
}