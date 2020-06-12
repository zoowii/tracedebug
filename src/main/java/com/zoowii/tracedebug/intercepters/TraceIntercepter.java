package com.zoowii.tracedebug.intercepters;

import classinjector.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Slf4j
@Component
public class TraceIntercepter implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String traceId = request.getParameter("TRACE_ID");
        if(!StringUtils.isEmpty(traceId)) {
            TraceContext.setCurrentTraceId(traceId);
            log.info("current traceId={}", traceId);
        } else {
            TraceContext.setCurrentTraceId(UUID.randomUUID().toString());
        }
        return true;
    }

}
