package com.jarvis.yh.utils.monitor;

import com.alibaba.fastjson.JSON;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Aspect
public class MonitorAspect {


    private static Logger logger = LoggerFactory.getLogger(MonitorAspect.class);

    @Pointcut("@annotation(Monitor) || @within(Monitor)")
    public void monitorPointCut() {

    }

    @Around("monitorPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object proceed = joinPoint.proceed();
            log(joinPoint, start, proceed, null);
            return proceed;
        } catch (Exception e) {
            log(joinPoint, start, null, e);
            throw e;
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

            Set<Integer> ignoreArgIndexSet = new HashSet<>();
            Monitor monitor = (Monitor) signature.getDeclaringType().getAnnotation(Monitor.class);
            if (signature instanceof MethodSignature) {
                MethodSignature methodSignature = (MethodSignature) signature;
                Monitor methodMonitor = methodSignature.getMethod().getDeclaredAnnotation(Monitor.class);
                if (methodMonitor != null) {
                    monitor = methodMonitor;
                }

                Parameter[] parameters = methodSignature.getMethod().getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    MonitorIgnore monitorIgnore = parameter.getAnnotation(MonitorIgnore.class);
                    if (monitorIgnore != null) {
                        ignoreArgIndexSet.add(i);
                    }
                }
            }

            if(monitor != null){
                timeout = monitor.timeout();
                printReq = monitor.printReq();
                printRes = monitor.printRes();
            }
            signatureName = signature.toString();

            if (printReq) {
                List objects = new ArrayList();
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
                        if (ignoreArgIndexSet.contains(i)
                                || arg instanceof InputStreamSource) {
                            objects.add(null);
                        } else {
                            objects.add(arg);
                        }
                    }
                }
                reqLog = JSON.toJSONString(objects);
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
