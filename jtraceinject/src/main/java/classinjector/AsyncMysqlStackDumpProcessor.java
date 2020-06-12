package classinjector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 类似MysqlStackDumpProcessor，但是把调试日志记录到数据库的步骤是加入到一个异步队列异步存储到mysql的
 */
public class AsyncMysqlStackDumpProcessor extends MysqlStackDumpProcessor {
    private Logger log = LoggerFactory.getLogger(AsyncMysqlStackDumpProcessor.class);

    private static ExecutorService executorService; // 这里最好是一个单线程的线程池，从而不会影响插入到数据库的id的顺序

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static void setExecutorService(ExecutorService executorService) {
        AsyncMysqlStackDumpProcessor.executorService = executorService;
    }

    @Override
    public void onSpanStart(String spanId, List<StackTraceElement> stackTrace, int stackDepth) {
        if(executorService==null) {
            log.warn(this.getClass().getName() + " not set executorService yet");
            return;
        }
        String traceId = TraceContext.getCurrentTraceId();
        log.info("span {} started in traceId {}", spanId, traceId);
        String moduleId = TraceContext.getModuleId(); // 模块ID，区分本span属于整个架构的哪个模块或者子服务

        executorService.submit(() -> {
            try {
                saveSpanStartToDb(moduleId, traceId, spanId, stackTrace, stackDepth);
            } catch (SQLException e) {
                e.printStackTrace();
                log.error("saveSpanStartToDb error", e);
            }
        });
    }

    @Override
    public void onDump(String spanId, int seqInSpan, String name, WeakReference<Object> valueRef, int lineNumber) {
        if(executorService==null) {
            log.warn(this.getClass().getName() + " not set executorService yet");
            return;
        }
        String traceId = TraceContext.getCurrentTraceId();
        Object value = valueRef.get();
        log.info("line {} span {}[{}] var {} value {}", lineNumber, spanId, seqInSpan, name, value);
        executorService.submit(() -> {
            try {
                saveDumpToDb(traceId, spanId, seqInSpan, name, valueRef, lineNumber);
            } catch (SQLException e) {
                e.printStackTrace();
                log.error("saveDumpToDb error", e);
            }
        });
    }
}
