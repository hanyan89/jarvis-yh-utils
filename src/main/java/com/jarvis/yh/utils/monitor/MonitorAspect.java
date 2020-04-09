package com.jarvis.yh.utils.monitor;

import com.alibaba.fastjson.JSON;
import com.jarvis.yh.utils.trace.TraceId;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Aspect
public class MonitorAspect {


    private static Logger logger = LoggerFactory.getLogger(MonitorAspect.class);

    @Pointcut("@annotation(Monitor) || @within(Monitor)")
    public void monitorPointCut() {

    }

    @Around("monitorPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        TraceId.set();
        MDC.put("traceId", TraceId.get());
        long start = System.currentTimeMillis();
        try {
            Object proceed = joinPoint.proceed();
            log(joinPoint, start, proceed, null);
            return proceed;
        } catch (Exception e) {
            log(joinPoint, start, null, e);
            throw e;
        } finally {
            MDC.remove("traceId");
        }
    }

    private void log(JoinPoint joinPoint, long start, Object result, Exception ex) {
        long end = System.currentTimeMillis();
        int timeout = 0;
        boolean printReq = true;
        boolean printRes = true;
        String reqLog = "";
        String resLog = "";
        String signatureName = "";
        Object[] args = null;
        try {
            args = joinPoint.getArgs();
            Signature signature = joinPoint.getSignature();

            Monitor monitor = (Monitor) signature.getDeclaringType().getAnnotation(Monitor.class);
            if (signature instanceof MethodSignature) {
                MethodSignature methodSignature = (MethodSignature) signature;
                Monitor methodMonitor = methodSignature.getMethod().getDeclaredAnnotation(Monitor.class);
                if (methodMonitor != null) {
                    monitor = methodMonitor;
                }
            }

            if(monitor != null){
                timeout = monitor.timeout();
                printReq = monitor.printReq();
                printRes = monitor.printRes();
            }
            signatureName = signature.toString();

            if (printReq) {
                reqLog = JSON.toJSONString(args);
            }
            if (printRes) {
                resLog = JSON.toJSONString(result);
            }
            logger.info("[{}],params:{},result:{},exception:{},costTime:{},{}",
                    signatureName, reqLog, resLog, ex, (end - start), slowRequest(start, end, timeout));
        } catch (Exception e) {
            logger.error("切面异常，[{}],params:{},result:{},exception:{}",
                    signatureName, args, resLog, ex, e);
        }
    }

    public String slowRequest(long start, long end, int timeout) {
        if (end - start > timeout) {
            return "slowRequest:".concat(String.valueOf(end - start));
        }
        return "";
    }
}
