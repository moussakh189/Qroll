package com.qroll.exception;

public class ExpiredQRException extends RuntimeException {
    public ExpiredQRException(String message )
    {
        super(message);
    }

    public ExpiredQRException(String  message , Throwable cause )
    {
        super(message , cause);
    }
}
