package com.zoowii.tracedebug.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Slf4j
@Aspect
public class TraceAspect {
    @Around("@annotation(com.zoowii.tracedebug.aspects.DebugTrace)")
    public void aroundDebugTraceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // TODO: profiler and get stacktrace, 方法中每条字节码执行前都记录下局部变量的变化
        // TODO: @DebugTrace包括的方法要字节码增强产生新类和新方法，开头和每条return前快照变量的变化，以及记录stacktrace到链路日志
        log.info("before joinpoint");
        joinPoint.proceed();

        log.info("after joinpoint");
    }
}
