package com.zoowii.tracedebug.configs;

import com.zoowii.tracedebug.intercepters.TraceIntercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.annotation.Resource;

@Configuration
public class MvcConfig extends WebMvcConfigurationSupport {
    @Resource
    private TraceIntercepter traceIntercepter;

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIntercepter).addPathPatterns("/**").excludePathPatterns("/actuator/**");
    }

}
