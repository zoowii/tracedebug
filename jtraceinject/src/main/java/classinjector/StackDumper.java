package classinjector;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StackDumper {

    private static List<IStackDumpProcessor> dumpProcessors = new ArrayList<IStackDumpProcessor>();

    static {
        TraceContext.load();

        ServiceLoader<IStackDumpProcessor> dumpProcessorServiceLoader = ServiceLoader.load(IStackDumpProcessor.class);
        for (IStackDumpProcessor processor : dumpProcessorServiceLoader) {
            try {
                dumpProcessors.add(processor);
            } catch (Exception e) {
                e.printStackTrace();
                // sometimes some providers will throw not found. eg. provider instances in test classes
                break;
            }
        }
    }

    public static List<IStackDumpProcessor> getDumpProcessors() {
        return dumpProcessors;
    }

    public static void setDumpProcessors(List<IStackDumpProcessor> processors) {
        dumpProcessors = processors;
    }

    // 当前trace是否开启了debug trace
    public static boolean isDebugTraceEnabledTrace() {
        if (dumpProcessors.isEmpty()) {
            return false;
        }
        return dumpProcessors.get(0).isDebugTraceEnabledTrace();
    }

    // spanId中第几次调用的序列号
    // TODO: 超时或者结束的spanId从内存中删除
    private static ConcurrentHashMap<String, AtomicInteger> spanStepSeqs
            = new ConcurrentHashMap<>(); // spanId => sequence in span

    /**
     * 判断一个栈帧是否是太深的框架内部的栈帧，或者是所有trace的方法都会有的很深的栈帧
     */
    private static boolean isNeedIgnoreInternalStackTraceElement(StackTraceElement stackTraceElement) {
        String className = stackTraceElement.getClassName();
        String methodName = stackTraceElement.getMethodName();
        if (className.startsWith("java.base/jdk.internal.reflect")) {
            return true;
        }
        if (className.startsWith("jdk.internal.reflect")) {
            return true;
        }
        if (className.startsWith("org.junit.runners")) {
            return true;
        }
        return false;
    }

    /**
     * 一次函数调用的开头
     */
    public static String spanStart() {
        String spanId = UUID.randomUUID().toString();
        if (!dumpProcessors.isEmpty()) {
            Thread t = Thread.currentThread();
            StackTraceElement[] rawStackTrace = t.getStackTrace();
            List<StackTraceElement> stackTraceElements = new ArrayList<>();

            // 因为深度限制，所以需要从某个stackTraceElement(className+methodName)作为顶层开始计算栈的深度
            for (StackTraceElement stackTraceElement : rawStackTrace) {
                if (isNeedIgnoreInternalStackTraceElement(stackTraceElement)) {
                    break;
                }
                stackTraceElements.add(stackTraceElement);
            }
            // stackTrace第一层是 Thread.getStackTrace，需要排除
            if (!stackTraceElements.isEmpty()) {
                stackTraceElements.remove(0);
            }
            int stackDepth = stackTraceElements.size();
            for (IStackDumpProcessor processor : dumpProcessors) {
                processor.onSpanStart(spanId, stackTraceElements, stackDepth);
            }
        }
        spanStepSeqs.put(spanId, new AtomicInteger(0));
        return spanId;
    }

    /**
     * 导出调用栈的变量信息
     */
    public static void dump(String spanId, String name, Object value, int lineNumber) {
        if (dumpProcessors.isEmpty()) {
            return;
        }
        // 记录本dump是本spanId的第几次调用，用来调试时回溯执行过程
        int seqInSpan = 0;
        AtomicInteger seqRef = spanStepSeqs.get(spanId);
        if (seqRef != null) {
            seqInSpan = seqRef.getAndIncrement();
        }

        // TODO: 判断值是否基本类型或者包装类型，如果是，并且值相比本spanId上次调用没有变化，忽略这个dump，避免数据量太大

        WeakReference<Object> valueRef = new WeakReference<Object>(value);
        for (IStackDumpProcessor processor : dumpProcessors) {
            processor.onDump(spanId, seqInSpan, name, valueRef, lineNumber);
        }
    }

}
