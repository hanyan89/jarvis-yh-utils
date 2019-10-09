package com.jarvis.yh.utils.idempotent;

import com.jarvis.yh.utils.exception.IdempotentException;
import com.jarvis.yh.utils.monitor.MonitorAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@Aspect
public class IdempotentAspect {

    private static Logger logger = LoggerFactory.getLogger(MonitorAspect.class);

    @Resource
    private StringRedisTemplate redisTemplate;

    public static final String IDEMPOTENT_PREFIX = "idempotent_";

    @Pointcut("@annotation(Idempotent) || @within(Idempotent)")
    public void idempotentPointCut() {

    }

    @Around("idempotentPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Idempotent idempotent = (Idempotent) joinPoint.getSignature().getDeclaringType().getAnnotation(Idempotent.class);
        String key = IDEMPOTENT_PREFIX + idempotent.idempotentKey();
        if (key == null || key.length() == 0) {
            throw new IdempotentException("幂等键不能为空");
        }
        long now = System.currentTimeMillis();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, Long.toString(now), idempotent.days(), idempotent.timeUnit());
        if (!success) {
            throw new IdempotentException("重复调用");
        }
        try {
            Object proceed = joinPoint.proceed();
            return proceed;
        } catch (Exception e) {
            throw e;
        }
    }
}
