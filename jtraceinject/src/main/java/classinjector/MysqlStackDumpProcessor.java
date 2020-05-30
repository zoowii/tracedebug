package classinjector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MysqlStackDumpProcessor extends DemoStackDumpProcessor {
    private Logger log = LoggerFactory.getLogger(MysqlStackDumpProcessor.class);

    private final DataSource dataSource;

    public MysqlStackDumpProcessor() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        // 连接到mysql
        String dbUrl = System.getProperty("DATABASE_URL");
        log.info("MysqlStackDumpProcessor DATABASE_URL property is {}", dbUrl);
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new RuntimeException("db url empty");
        }
        if (!dbUrl.startsWith("jdbc")) {
            throw new RuntimeException("invali db url");
        }
        String dbUser = System.getProperty("DB_USER");
        String dbPass = System.getProperty("DB_PASS");

        Class.forName("com.mysql.cj.jdbc.Driver");
        Class dataSourceCls = Class.forName("com.mysql.cj.jdbc.MysqlConnectionPoolDataSource");
        this.dataSource = (DataSource) dataSourceCls.getDeclaredConstructor().newInstance();
        Method setUrlMethod = dataSource.getClass().getMethod("setUrl", String.class);
        if (setUrlMethod != null) {
            setUrlMethod.invoke(dataSource, dbUrl);
        }
        Method setUserMethod = dataSource.getClass().getMethod("setUser", String.class);
        if (setUserMethod != null) {
            setUserMethod.invoke(dataSource, dbUser);
        }
        Method setPasswordMethod = dataSource.getClass().getMethod("setPassword", String.class);
        if (setPasswordMethod != null) {
            setPasswordMethod.invoke(dataSource, dbPass);
        }
    }

    private int maxStackTraceDepth = 5;

    // TODO: traceStart的时候产生traceId
    String traceId = "test" + new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date()); // TODO

    @Override
    public void onSpanStart(String spanId, List<StackTraceElement> stackTrace, int stackDepth) {
        System.out.println("span " + spanId + " started in traceId " + traceId);
        // TODO: 从当前线程或者请求参数获取traceId(如果没有，返回)。记录spanId和traceId映射关系
        if (dataSource == null) {
            return;
        }
        String moduleId = "test"; // 模块ID，区分本span属于整个架构的哪个模块或者子服务

        try {
            String clsName = null;
            String methodName = null;
            if (stackTrace.size() >= 2) {
                // stackTrace第一层是字节码增强后的类，第二层才是原类
                StackTraceElement stackTraceElement = stackTrace.get(1);
                clsName = stackTraceElement.getClassName();
                methodName = stackTraceElement.getMethodName();
            }

            Connection conn = dataSource.getConnection();
            try {
                {
                    PreparedStatement pstm = conn.prepareStatement("insert into trace_span" +
                            " (trace_id, span_id, module_id, classname, method_name, stack_depth)" +
                            " values (?, ?, ?, ?, ?, ?)");
                    try {
                        pstm.setString(1, traceId);
                        pstm.setString(2, spanId);
                        pstm.setString(3, moduleId);
                        pstm.setString(4, clsName);
                        pstm.setString(5, methodName);
                        pstm.setInt(6, stackDepth);
                        pstm.execute();
                    } finally {
                        pstm.close();
                    }
                }

                // span的stackTrace也要保存到数据库. 从索引1开始，因为1是spanStart方法
                int ignoreStackSize = 1;
                for (int i = ignoreStackSize; i < stackTrace.size(); i++) {
                    if (i >= maxStackTraceDepth) {
                        break;
                    }
                    StackTraceElement stackTraceElement = stackTrace.get(i);

                    PreparedStatement pstm = conn.prepareStatement("insert into span_stack_trace" +
                            " (trace_id, span_id," +
                            " stack_index, module_id, classname, method_name, line, filename)" +
                            " values (?, ?, ?, ?, ?, ?, ?, ?)");
                    try {
                        pstm.setString(1, traceId);
                        pstm.setString(2, spanId);
                        pstm.setInt(3, i - ignoreStackSize);
                        pstm.setString(4, moduleId);
                        pstm.setString(5, stackTraceElement.getClassName());
                        pstm.setString(6, stackTraceElement.getMethodName());
                        pstm.setInt(7, stackTraceElement.getLineNumber());
                        pstm.setString(8, stackTraceElement.getFileName());
                        pstm.execute();
                    } finally {
                        pstm.close();
                    }
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDump(String spanId, int seqInSpan, String name, WeakReference<Object> valueRef, int lineNumber) {
        Object value = valueRef.get();
        System.out.println("line " + lineNumber + " span " + spanId + "[" + seqInSpan + "] var " + name + " value " + value);

        if (dataSource == null) {
            return;
        }
        try {
            Connection conn = dataSource.getConnection();
            try {
                PreparedStatement pstm = conn.prepareStatement(
                        "insert into span_dump_item (trace_id, span_id, seq_in_span, `name`, `value`, line)" +
                                " values (?, ?, ?, ?, ?, ?)");
                try {
                    pstm.setString(1, traceId);
                    pstm.setString(2, spanId);
                    pstm.setInt(3, seqInSpan);
                    pstm.setString(4, name);
                    pstm.setString(5, "" + value);
                    pstm.setInt(6, lineNumber);
                    pstm.execute();
                } finally {
                    pstm.close();
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
