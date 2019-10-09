package com.jarvis.yh.utils.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({ElementType.METHOD})
public @interface Idempotent {
    String idempotentKey() default "";

    int days() default 10;

    TimeUnit timeUnit() default TimeUnit.DAYS;
}
