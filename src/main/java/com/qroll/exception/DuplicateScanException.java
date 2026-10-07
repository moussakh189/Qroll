package com.qroll.exception;

public class DuplicateScanException  extends RuntimeException
// needed later
{
    public DuplicateScanException(String message )
    {
        super(message);
    }

    public DuplicateScanException(String message , Throwable cause )
    {
        super(message , cause );
    }
}
