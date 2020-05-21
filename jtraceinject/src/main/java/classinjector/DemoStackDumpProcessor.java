package classinjector;

import classinjector.IStackDumpProcessor;

import java.lang.ref.WeakReference;
import java.util.List;

public class DemoStackDumpProcessor implements IStackDumpProcessor {
    // TODO: 保存trace信息到日志服务或者数据库，以及提供API和界面用来回溯断点调试

    private ThreadLocal<Boolean> debugTraceEnabled = new ThreadLocal<Boolean>();

    @Override
    public boolean isDebugTraceEnabledTrace() {
        Boolean enable = debugTraceEnabled.get();
        if (enable != null) {
            return enable;
        }
        // 实际场景需要根据用户和概率采样来开启debug trace
        debugTraceEnabled.set(true);
        return true;
    }

    @Override
    public void onSpanStart(String spanId, List<StackTraceElement> stackTrace, int stackDepth) {
        System.out.println("span " + spanId + " started with stack depth " + stackDepth);
    }

    @Override
    public void onDump(String spanId, int seqInSpan, String name, WeakReference<Object> valueRef, int lineNumber) {
        Object value = valueRef.get();
        System.out.println("line " + lineNumber + " span " + spanId + "[" + seqInSpan + "] var " + name + " value " + value);
    }
}
