package com.qroll.exception;

public class SessionNotActiveException extends RuntimeException {
    public SessionNotActiveException(String message) {
        super(message);
    }

    public SessionNotActiveException(String message, Throwable cause) { super(message, cause); }
}
