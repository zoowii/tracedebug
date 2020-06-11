package classinjector;

public class TraceDumpOptions {
    private String moduleId;
    private String currentTraceId;

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getCurrentTraceId() {
        return currentTraceId;
    }

    public void setCurrentTraceId(String currentTraceId) {
        this.currentTraceId = currentTraceId;
    }
}
