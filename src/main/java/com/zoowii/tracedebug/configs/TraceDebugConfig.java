package com.zoowii.tracedebug.configs;

import classinjector.MysqlStackDumpProcessor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Getter
@Setter
@Component
public class TraceDebugConfig {
    @Value("${tracedebug.datasource.url}")
    private String url;
    @Value("${tracedebug.datasource.username}")
    private String username;
    @Value("${tracedebug.datasource.password}")
    private String password;
    @Value("${tracedebug.moduleId}")
    private String moduleId;

    @PostConstruct
    public void afterInit() {
        MysqlStackDumpProcessor.setDbOptions(url, username, password);
        MysqlStackDumpProcessor.setTraceDumpModuleId(moduleId);
    }
}
