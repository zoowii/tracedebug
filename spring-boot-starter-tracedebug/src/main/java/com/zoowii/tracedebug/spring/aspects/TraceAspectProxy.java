package com.zoowii.tracedebug.spring.aspects;

import classinjector.ClassInjector;
import classinjector.ITraceInjected;
import classinjector.TraceInjectedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TraceAspectProxy implements ApplicationContextAware, BeanFactoryPostProcessor,
        BeanDefinitionRegistryPostProcessor {
    private Logger log = LoggerFactory.getLogger(TraceAspectProxy.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private final ClassInjector injector = new ClassInjector();

    // TODO: 增加一个spring boot starter模块方便快速使用

    private boolean isDebugTraceEnhancedClass(Class<?> beanCls) {
        Set<String> beanClsInterfaceNames = new HashSet<>();
        for (Class<?> interfaceCls : beanCls.getInterfaces()) {
            beanClsInterfaceNames.add(interfaceCls.getName());
        }
        if (beanCls.getDeclaredAnnotation(TraceInjectedType.class) != null
                || beanClsInterfaceNames.contains(ITraceInjected.class.getName())) {
            return true;
        }
        return false;
    }

    // 判断是否类型上或者类型中的方法中有 @DebugTrace注解
    private boolean hasDebugTraceInTypeOrMethod(Class<?> beanCls) {
        if (beanCls.getDeclaredAnnotation(DebugTrace.class) != null) {
            return true;
        }
        for (Method method : beanCls.getDeclaredMethods()) {
            if (method.getDeclaredAnnotation(DebugTrace.class) != null) {
                return true;
            }
        }
        return false;
    }

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
                if (clsName.contains("$$EnhancerBySpringCGLIB$$")) {
                    continue;
                }
                beanCls = Class.forName(clsName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                log.error("load class " + clsName + " error", e);
                continue;
            }
            // 已经是debugtrace产生的类不需要再增强，并且要取消这个bean definition
            if (isDebugTraceEnhancedClass(beanCls)) {
                log.info("remove injected bean definition {}", defName);
                defaultListableBeanFactory.removeBeanDefinition(defName);
                continue;
            }

            if (!hasDebugTraceInTypeOrMethod(beanCls)) {
                // 没有@DebugTrace注解过的类或方法不需要trace增强
                continue;
            }
            log.info("found debugtrace annotated class {}", beanCls.getName());
            // 给这个类trace增强
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
                Class<?> generatedProxyCls = injector.addDumpToMethods(beanCls,
                        traceMethods,
                        beanCls.getName() + "_proxy");
                log.info("generated debugtrace proxy class {} definition name {}",
                        generatedProxyCls.getName(), defName);
                GenericBeanDefinition proxyBeanDefinition = new GenericBeanDefinition(beanDefinition);
                proxyBeanDefinition.setBeanClass(generatedProxyCls);
                proxyBeanDefinition.setBeanClassName(generatedProxyCls.getName());
                log.info("remove bean definition {}", defName);
                defaultListableBeanFactory.removeBeanDefinition(defName);
                log.info("add bean definition {} class {}", defName, generatedProxyCls.getName());
                defaultListableBeanFactory.registerBeanDefinition(defName, proxyBeanDefinition);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("trace aspect enhancer error", e);
            }
        }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {

    }
}
