package classinjector;

import java.lang.ref.WeakReference;
import java.util.List;

public interface IStackDumpProcessor {

    // 当前trace是否开启了debug trace
    boolean isDebugTraceEnabledTrace();

    // 一次函数调用开始时的回调
    void onSpanStart(String spanId, List<StackTraceElement> stackTrace, int stackDepth);

    // 导出某个变量的信息的回调
    void onDump(String spanId, int seqInSpan, String name, WeakReference<Object> value, int lineNumber);
}
