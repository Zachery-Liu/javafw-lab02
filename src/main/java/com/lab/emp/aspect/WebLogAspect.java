package com.lab.emp.aspect;

import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class WebLogAspect {
    private static final Logger LOG = LoggerFactory.getLogger(WebLogAspect.class);

    @Around("execution(public * com.lab.emp.controller..*(..))")
    public Object logRequest(ProceedingJoinPoint call) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String method = attributes.getRequest().getMethod();
        String endpoint = call.getSignature().toShortString();
        long start = System.nanoTime();
        try {
            Object response = call.proceed();
            LOG.info("{} {} completed in {} ms", method, endpoint, elapsedMillis(start));
            return response;
        } catch (Throwable exception) {
            LOG.warn("{} {} failed in {} ms ({})", method, endpoint, elapsedMillis(start),
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private static long elapsedMillis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
