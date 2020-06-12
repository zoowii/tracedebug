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
import java.util.List;

public class MysqlStackDumpProcessor extends DemoStackDumpProcessor {
    private Logger log = LoggerFactory.getLogger(MysqlStackDumpProcessor.class);

    private DataSource dataSource;

    private static DumpDbOptions options;

    static {
        // init dbOptions
        options = new DumpDbOptions();
        options.setDbUrl(System.getProperty("DATABASE_URL"));
        options.setUsername(System.getProperty("DB_USER"));
        options.setPassword(System.getProperty("DB_PASS"));
        options.setDriver(System.getProperty("DB_DRIVER", "com.mysql.cj.jdbc.Driver"));
    }

    public static void setDbOptions(String dbUrl, String dbUser, String dbPassword) {
        options.setDbUrl(dbUrl);
        options.setUsername(dbUser);
        options.setPassword(dbPassword);
    }

    private DataSource getDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        // 连接到mysql
        try {
            String dbUrl = options.getDbUrl();
            log.info("MysqlStackDumpProcessor db url is {}", dbUrl);
            if (dbUrl == null || dbUrl.isEmpty()) {
                throw new RuntimeException("db url empty");
            }
            if (!dbUrl.startsWith("jdbc")) {
                throw new RuntimeException("invalid db url");
            }
            String dbUser = options.getUsername();
            String dbPass = options.getPassword();

            Class.forName(options.getDriver());
            Class<?> dataSourceCls = Class.forName("com.mysql.cj.jdbc.MysqlConnectionPoolDataSource");
            this.dataSource = (DataSource) dataSourceCls.getDeclaredConstructor().newInstance();
            Method setUrlMethod = dataSource.getClass().getMethod("setUrl", String.class);
            setUrlMethod.invoke(dataSource, dbUrl);
            Method setUserMethod = dataSource.getClass().getMethod("setUser", String.class);
            setUserMethod.invoke(dataSource, dbUser);
            Method setPasswordMethod = dataSource.getClass().getMethod("setPassword", String.class);
            setPasswordMethod.invoke(dataSource, dbPass);
            return this.dataSource;
        } catch (ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private int maxStackTraceDepth = 5;

    // TODO: traceStart的时候产生traceId

    protected void saveSpanStartToDb(String moduleId, String traceId,
                                     String spanId, List<StackTraceElement> stackTrace,
                                     int stackDepth) throws SQLException {
        DataSource db = getDataSource();
        if (db == null) {
            return;
        }
        String clsName = null;
        String methodName = null;
        if (stackTrace.size() >= 2) {
            // stackTrace第一层是字节码增强后的类，第二层才是原类
            StackTraceElement stackTraceElement = stackTrace.get(1);
            clsName = stackTraceElement.getClassName();
            methodName = stackTraceElement.getMethodName();
        }

        Connection conn = db.getConnection();
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
    }

    @Override
    public void onSpanStart(String spanId, List<StackTraceElement> stackTrace, int stackDepth) {
        String traceId = TraceContext.getCurrentTraceId();
        log.info("span {} started in traceId {}", spanId, traceId);
        String moduleId = TraceContext.getModuleId(); // 模块ID，区分本span属于整个架构的哪个模块或者子服务

        try {
            saveSpanStartToDb(moduleId, traceId, spanId, stackTrace, stackDepth);
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("saveSpanStartToDb error", e);
        }
    }

    protected void saveDumpToDb(String traceId, String spanId, int seqInSpan,
                                String name, WeakReference<Object> valueRef, int lineNumber)
            throws SQLException {
        DataSource db = getDataSource();
        if (db == null) {
            return;
        }
        Object value = valueRef.get();
        Connection conn = db.getConnection();
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
    }

    @Override
    public void onDump(String spanId, int seqInSpan, String name, WeakReference<Object> valueRef, int lineNumber) {
        String traceId = TraceContext.getCurrentTraceId();
        Object value = valueRef.get();
        log.info("line {} span {}[{}] var {} value {}", lineNumber, spanId, seqInSpan, name, value);
        try {
            saveDumpToDb(traceId, spanId, seqInSpan, name, valueRef, lineNumber);
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("saveDumpToDb error", e);
        }
    }
}
