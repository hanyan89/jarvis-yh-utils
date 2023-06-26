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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Aspect
public class DeprecatedMarker implements ApplicationContextAware , InitializingBean{

    private static Logger logger = LoggerFactory.getLogger(DeprecatedMarker.class);

    private ApplicationContext applicationContext;

    //记录开始时间，因为没有持久化，约等于项目最后一次启动时间
    private Date star;

    private ConcurrentHashMap<String, AtomicInteger> context = new ConcurrentHashMap();

    public List<String> all() {
        List<String> list = new ArrayList<>();
        String dateRange = String.format("开始时间:%s -> 结束时间:%s", formatDate(star), formatDate(new Date()));
        list.add(dateRange);
        List<String> all = context.entrySet().stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .map(entry -> String.format("%s -> %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        list.addAll(all);
        return list;
    }

    public List<String> deprecated() {
        List<String> list = new ArrayList<>();
        String dateRange = String.format("开始时间:%s -> 结束时间:%s", formatDate(star), formatDate(new Date()));
        list.add(dateRange);
        List<String> all = context.entrySet().stream()
                .filter(entry -> entry.getValue().get() == 0)
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
        list.addAll(all);
        return list;
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    @Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
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
            AtomicInteger atomicInteger = context.get(signature.toLongString());
            if (atomicInteger != null) {
                atomicInteger.incrementAndGet();
            }
        } catch (Exception e) {
            logger.error("标记接口失败", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(Controller.class);
        for (Map.Entry<String, Object> entry : controllers.entrySet()) {
            Object proxyController = entry.getValue();
            Class<?> controllerClass = ClassUtils.getUserClass(proxyController);
            Method[] methods = controllerClass.getMethods();
            for (Method method : methods) {
                RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
                if (annotation != null) {
                    context.put(method.toString(), new AtomicInteger(0));
                }
            }
        }
        star = new Date();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
