package classinjector;

import classinjector.IStackDumpProcessor;

import java.lang.ref.WeakReference;
import java.util.List;

public class DemoStackDumpProcessor implements IStackDumpProcessor {
    private final ThreadLocal<Boolean> debugTraceEnabled = new ThreadLocal<>();

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
