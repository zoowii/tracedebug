package com.zoowii.tracedebug.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Aspect
public class TraceAspect {
    @Around("@annotation(com.zoowii.tracedebug.aspects.DebugTrace)")
    public void aroundDebugTraceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("before joinpoint");
        joinPoint.proceed();
        log.info("after joinpoint");
    }
}
