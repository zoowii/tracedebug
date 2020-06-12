package com.zoowii.tracedebug.components;

import classinjector.AsyncMysqlStackDumpProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class TraceDebugDbExecutor implements InitializingBean {


    private ExecutorService singletonExecutor;

    @Override
    public void afterPropertiesSet() throws Exception {
        singletonExecutor = Executors.newSingleThreadExecutor();
        AsyncMysqlStackDumpProcessor.setExecutorService(singletonExecutor);
    }

    @PreDestroy
    public void shutdown() {
        if(singletonExecutor!=null) {
            singletonExecutor.shutdown();
        }
    }
}
