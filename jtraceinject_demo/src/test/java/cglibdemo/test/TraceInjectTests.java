package cglibdemo.test;

import cglibdemo.Dao;
import classinjector.ClassInjector;
import org.junit.Test;

import java.util.Arrays;

public class TraceInjectTests {

    @Test
    public void testCglib() throws Exception {
        String mysqlHost = "127.0.0.1"; // "192.168.1.220";
        int mysqlPort = 3306;
        String dbName = "debug_trace_dev";
        String dbUrl = String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf-8&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC", mysqlHost, mysqlPort, dbName);
        String username = "root";
        String password = "123456"; // "yqr@2017";
        System.setProperty("DATABASE_URL", dbUrl);
        System.setProperty("DB_USER", username);
        System.setProperty("DB_PASS", password);

        ClassInjector injector = new ClassInjector();
        Class<? extends Dao> generatedProxyCls = injector.addDumpToMethods(Dao.class,
                Arrays.asList(Dao.class.getMethod("update", Integer.class)
                        , Dao.class.getMethod("sum", int.class, int.class)
                        , Dao.class.getMethod("hi")
                ),
                Dao.class.getName() + "_proxy");

        Dao proxyObj = generatedProxyCls.getConstructor().newInstance();
        System.out.println("proxy object called");
        proxyObj.update(234);

        System.out.println("sum(1,2)=" + proxyObj.sum(1, 2));

        System.out.println("start call hi");
        proxyObj.hi();

//        Enhancer enhancer = new Enhancer();
//        enhancer.setSuperclass(Dao.class);
//        enhancer.setCallback(daoProxy);


//        Dao dao = (Dao)enhancer.create();
//        dao = (Dao)enhancer.create();
//        dao.update(123);
    }
}
