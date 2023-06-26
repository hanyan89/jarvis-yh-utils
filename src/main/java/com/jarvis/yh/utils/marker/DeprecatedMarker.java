package com.jarvis.yh.utils.marker;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class DeprecatedMarker implements ApplicationContextAware , InitializingBean{

    private static Logger logger = LoggerFactory.getLogger(DeprecatedMarker.class);

    private ApplicationContext applicationContext;

    private ConcurrentHashMap<String, AtomicInteger> context = new ConcurrentHashMap();

    @Pointcut("@within(org.springframework.stereotype.Controller)")
    public void pointCut() {

    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();
        mark(joinPoint);
        return proceed;
    }

    private void mark(ProceedingJoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            String className = signature.getDeclaringTypeName();
            String methodName = signature.getName();
            logger.info("{}{}", className, methodName);
        } catch (Exception e) {
            logger.error("标记接口失败", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(Controller.class);
        for (Map.Entry<String, Object> entry : controllers.entrySet()) {
//            String controllerName = entry.getKey();
            Object controller = entry.getValue();
            String controllerName = controller.getClass().getName();
            Method[] methods = controller.getClass().getMethods();
            for (Method method : methods) {
                Annotation[] annotations = method.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation.getClass().getSimpleName().equals("RequestMapping")) {
                        String key = controllerName.concat(".").concat(method.getName());
                        context.put(key, new AtomicInteger(0));
                    }
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
