package com.zoowii.tracedebug.spring.intercepters;

import classinjector.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Component
public class TraceIntercepter implements HandlerInterceptor {
    private Logger log = LoggerFactory.getLogger(TraceIntercepter.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String traceId = request.getParameter("TRACE_ID");
        if (!StringUtils.isEmpty(traceId)) {
            TraceContext.setCurrentTraceId(traceId);
            log.info("current traceId={}", traceId);
        } else {
            TraceContext.setCurrentTraceId(UUID.randomUUID().toString());
        }
        return true;
    }

}
