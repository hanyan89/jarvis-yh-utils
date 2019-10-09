package com.jarvis.yh.utils.idempotent;

import com.jarvis.yh.utils.exception.IdempotentException;
import com.jarvis.yh.utils.monitor.MonitorAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import javax.annotation.Resource;
import java.lang.reflect.Method;

@Aspect
public class IdempotentAspect {

    private static Logger logger = LoggerFactory.getLogger(MonitorAspect.class);

    private SpelExpressionParser parser = new SpelExpressionParser();

    @Resource
    private RedisTemplate redisTemplate;

    public static final String IDEMPOTENT_PREFIX = "idempotent_";

    @Pointcut("@annotation(Idempotent) || @within(Idempotent)")
    public void idempotentPointCut() {

    }

    @Around("idempotentPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        Expression expression = parser.parseExpression(idempotent.idempotentKey());
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] paramNames = discoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        String value = expression.getValue(context).toString();
        if (value == null || value.length() == 0) {
            throw new IdempotentException("幂等键不能为空");
        }
        String key = IDEMPOTENT_PREFIX + value;
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
