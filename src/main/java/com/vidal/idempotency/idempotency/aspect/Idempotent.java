package com.vidal.idempotency.idempotency.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    int ttlMinutes() default 5;
}