package com.zoowii.tracedebug.intercepters;

import classinjector.MysqlStackDumpProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class TraceIntercepter implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String traceId = request.getParameter("TRACE_ID");
        if(!StringUtils.isEmpty(traceId)) {
            MysqlStackDumpProcessor.setCurrentTraceId(traceId);
            log.info("current traceId={}", traceId);
        }
        return true;
    }

}
