package com.zoowii.tracedebug.spring.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import classinjector.AsyncMysqlStackDumpProcessor;
import classinjector.TraceContext;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(TraceDebugStarterProperties.class)
public class TraceDebugAutoConfig {
    private Logger log = LoggerFactory.getLogger(TraceDebugAutoConfig.class);

    @Resource
    private TraceDebugStarterProperties traceDebugStarterProperties;
    
    @PostConstruct
    public void afterInit() {
        AsyncMysqlStackDumpProcessor.setDbOptions(
            traceDebugStarterProperties.getDatasource().getUrl(),
            traceDebugStarterProperties.getDatasource().getUsername(),
            traceDebugStarterProperties.getDatasource().getPassword());
        String moduleId = traceDebugStarterProperties.getModuleId();
        TraceContext.setTraceDumpModuleId(moduleId);
        log.info("inited TraceDebugStarterProperties moduleId {}", moduleId);
    }
}