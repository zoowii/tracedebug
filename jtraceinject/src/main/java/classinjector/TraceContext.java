package classinjector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TraceContext {
    private static final Logger log = LoggerFactory.getLogger(TraceContext.class);

    private static TraceDumpOptions traceDumpOptions;

    static {
        // init traceDumpOptions
        traceDumpOptions = new TraceDumpOptions();
        traceDumpOptions.setModuleId("java");
        traceDumpOptions.setCurrentTraceId("test" + new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date()));
    }

    public static void setTraceDumpModuleId(String moduleId) {
        traceDumpOptions.setModuleId(moduleId);
    }

    public static void setCurrentTraceId(String traceId) {
        traceDumpOptions.setCurrentTraceId(traceId);
    }

    public static String getCurrentTraceId() {
        return traceDumpOptions.getCurrentTraceId();
    }

    public static String getModuleId() {
        return traceDumpOptions.getModuleId();
    }


    public static void load() {
        // must be called to load TraceContext class static init method
        log.info("TraceContext loaded");
    }
}
