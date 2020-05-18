package com.zoowii.tracedebug.exceptions;

public class SpanNotFoundException extends Exception {
    public SpanNotFoundException() {
    }

    public SpanNotFoundException(String message) {
        super(message);
    }

    public SpanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpanNotFoundException(Throwable cause) {
        super(cause);
    }

    public SpanNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
