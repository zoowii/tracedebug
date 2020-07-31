package com.zoowii.tracedebug.spring.executors;

import classinjector.AsyncMysqlStackDumpProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TraceDebugDbExecutor implements InitializingBean {
    private Logger log = LoggerFactory.getLogger(TraceDebugDbExecutor.class);

    private ExecutorService singletonExecutor;

    @Override
    public void afterPropertiesSet() throws Exception {
        singletonExecutor = Executors.newSingleThreadExecutor();
        AsyncMysqlStackDumpProcessor.setExecutorService(singletonExecutor);
    }

    @PreDestroy
    public void shutdown() {
        if (singletonExecutor != null) {
            singletonExecutor.shutdown();
        }
    }
}
