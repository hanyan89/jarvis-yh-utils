package com.jarvis.yh.utils.trace;

import java.util.UUID;

public class TraceId {

    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static String get() {
        return threadLocal.get();
    }

    public static void set() {
        String traceId = UUID.randomUUID().toString().replaceAll("-", "");
        threadLocal.set(traceId);
    }

    public static void remove() {
        threadLocal.remove();
    }
}
