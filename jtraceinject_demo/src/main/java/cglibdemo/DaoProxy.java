package cglibdemo;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class DaoProxy implements MethodInterceptor {
    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("Before Method Invoke");
        proxy.invokeSuper(object, args);
        Thread t = Thread.currentThread();
        StackTraceElement[] stack = t.getStackTrace();
        for (StackTraceElement ele : stack) {
        }

        System.out.println("After Method Invoke");

        return object;
    }
}
