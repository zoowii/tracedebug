package com.zoowii.tracedebug.aspects;

import classinjector.ClassInjector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TraceAspectProxy implements ApplicationContextAware, BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private final ClassInjector injector = new ClassInjector();

    // TODO: 增加一个spring boot starter模块方便快速使用

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof DefaultListableBeanFactory)) {
            return;
        }

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (String defName : beanDefinitionNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(defName);
            String clsName = beanDefinition.getBeanClassName();
            if (clsName == null) {
                continue;
            }
            Class<?> beanCls;
            try {
                beanCls = Class.forName(clsName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }
            // 已经是debugtrace产生的类不需要再增强
            if (clsName.endsWith("_proxy")) {
                continue;
            }
            // TODO: 只要方法总有@DebugTrace也算
            DebugTrace clsTraceAnno = beanCls.getDeclaredAnnotation(DebugTrace.class);
            if (clsTraceAnno == null) {
                continue;
            }
            log.info("found debugtrace annotated class {}", beanCls.getName());
            // TODO: 给这个类trace增强
            Method[] methods = beanCls.getMethods();
            List<Method> traceMethods = new ArrayList<>();
            for (Method method : methods) {
                DebugTrace methodTraceAnno = method.getDeclaredAnnotation(DebugTrace.class);
                if (methodTraceAnno == null) {
                    continue;
                }
                traceMethods.add(method);
            }
            try {
                // TODO: originMethod需要去除注解(inject时传参数，避免@GetMapping等注解产生的歧义)
                Class<?> generatedProxyCls = injector.addDumpToMethods(beanCls,
                        traceMethods,
                        beanCls.getName() + "_proxy");
                log.info("generated debugtrace proxy class {}", generatedProxyCls.getName());
                GenericBeanDefinition proxyBeanDefinition = new GenericBeanDefinition(beanDefinition);
                proxyBeanDefinition.setBeanClass(generatedProxyCls);
                proxyBeanDefinition.setBeanClassName(generatedProxyCls.getName());
                defaultListableBeanFactory.removeBeanDefinition(defName);
                defaultListableBeanFactory.registerBeanDefinition(defName, proxyBeanDefinition);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {

    }
}
