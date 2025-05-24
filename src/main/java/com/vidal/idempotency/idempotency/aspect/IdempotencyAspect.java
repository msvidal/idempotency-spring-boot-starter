package com.vidal.idempotency.idempotency.aspect;

import com.vidal.idempotency.idempotency.model.IdempotencyEntry;
import com.vidal.idempotency.idempotency.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {

        String cacheKey = null;

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            cacheKey = request.getHeader("X-Idempotency-Key");
        }

        if (cacheKey == null) {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof org.springframework.messaging.Message<?>) {
                    org.springframework.messaging.Message<?> message = (org.springframework.messaging.Message<?>) arg;
                    cacheKey = (String) message.getHeaders().get("X-Idempotency-Key");
                    break;
                }
            }
        }

        if (cacheKey == null) {
            log.info("Nenhuma chave de idempotência encontrada, prosseguindo sem cache");
            return joinPoint.proceed();
        }

        IdempotencyEntry cachedEntry = idempotencyService.retrieve(cacheKey);
        if (cachedEntry != null) {
            log.info("Resultado de idempotência encontrado no cache para a chave: {}", cacheKey);
            return cachedEntry.result();
        }

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        Object body = null;

        if (result instanceof ResponseEntity<?>) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            body = responseEntity.getBody();
        } else {
            body = result;
        }
        long executionTime = System.currentTimeMillis() - startTime;

        idempotencyService.store(cacheKey, body, idempotent.ttlMinutes());

        log.info("Resultado armazenado para chave de idempotência: {} (tempo de execução: {}ms)", cacheKey, executionTime);

        return result;
    }
}