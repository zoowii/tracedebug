package classinjector;

public class DumpItemInfo {
    private String traceId;
    private String spanId;
    private Integer seqInSpan;
    private String name;
    private Object value;
    private Integer lineNumber;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public Integer getSeqInSpan() {
        return seqInSpan;
    }

    public void setSeqInSpan(Integer seqInSpan) {
        this.seqInSpan = seqInSpan;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
}
