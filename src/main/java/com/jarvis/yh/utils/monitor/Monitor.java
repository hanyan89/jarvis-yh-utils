package com.jarvis.yh.utils.monitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface Monitor {

    int timeout() default 1000;

    boolean printReq() default true;

    boolean printRes() default false;
}
