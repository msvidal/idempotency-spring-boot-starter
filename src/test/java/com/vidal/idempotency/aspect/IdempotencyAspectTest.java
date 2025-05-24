package com.vidal.idempotency.aspect;

import com.vidal.idempotency.config.IdempotencyAutoConfiguration;
import com.vidal.idempotency.idempotency.aspect.Idempotent;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class IdempotencyAspectTest {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyAspectTest.class);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("logging.level.com.vidal.idempotency.idempotency.aspect", () -> "DEBUG");
        registry.add("logging.level.com.vidal.idempotency.idempotency.service", () -> "DEBUG");
        registry.add("logging.level.org.springframework.test", () -> "INFO");
        registry.add("spring.output.ansi.enabled", () -> "ALWAYS");
    }

    @Autowired
    private TestService testService;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableAspectJAutoProxy
    @Import(IdempotencyAutoConfiguration.class)
    static class TestConfig {
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    static class TestService {
        private int callCount = 0;

        @Idempotent()
        public String processRequest(String id) {
            callCount++;
            return "Processed " + id + " " + callCount + " times";
        }

        public int getCallCount() {
            return callCount;
        }
    }

    @Test
    void shouldBlockDuplicateMethodCalls() {
        logger.info("Iniciando teste de idempotência");

        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("X-Idempotency-Key", "123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request1));

        String result1 = testService.processRequest("123");

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.addHeader("X-Idempotency-Key", "123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request2));
        String result2 = testService.processRequest("123");

        MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.addHeader("X-Idempotency-Key", "456");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request3));
        String result3 = testService.processRequest("456");

        RequestContextHolder.resetRequestAttributes();

        assertThat(testService.getCallCount()).isEqualTo(2);
        assertThat(result1).contains("Processed 123");
        assertThat(result2).isEqualTo(result1);
        assertThat(result3).contains("Processed 456");

        logger.info("Resultados: result1={}, result2={}, result3={}", result1, result2, result3);
    }}