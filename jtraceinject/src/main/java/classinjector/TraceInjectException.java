package classinjector;

public class TraceInjectException extends Exception {
    public TraceInjectException() {
    }

    public TraceInjectException(String message) {
        super(message);
    }

    public TraceInjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public TraceInjectException(Throwable cause) {
        super(cause);
    }

    public TraceInjectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
