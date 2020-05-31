package com.zoowii.tracedebug.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
@Aspect
public class RequestLogAspect {
    private String arrayToString(Object[] args) {
        if(args == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<args.length;i++) {
            if(i>0) {
                builder.append(", ");
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    @Around("@annotation(requestLog)")
    public Object aroundRequestLogMethod(ProceedingJoinPoint joinPoint, RequestLog requestLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
//        RequestLog requestLog = method.getDeclaredAnnotation(RequestLog.class);
        if(requestLog!=null && requestLog.request()) {
            log.info("api " + methodName + " args " + arrayToString(joinPoint.getArgs()));
        }
        Object response = joinPoint.proceed();
        if(requestLog!=null && requestLog.response()) {
            log.info("api " + methodName + " response " + response);
        }
        return response;
    }
}
