package classinjector;

public class TraceDumpOptions {
    private String moduleId;
    private ThreadLocal<String> currentTraceId = new ThreadLocal<>();

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getCurrentTraceId() {
        return currentTraceId.get();
    }

    public void setCurrentTraceId(String currentTraceId) {
        this.currentTraceId.set(currentTraceId);
    }
}
