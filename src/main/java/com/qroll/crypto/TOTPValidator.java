package com.qroll.crypto;

import com.qroll.exception.ExpiredQRException;

public class TOTPValidator {

    private  final TOTPEngine engine = new TOTPEngine();
    // EDGE CASES ?
    // SCAN AT 00:29 WITH DELAY --> RA7 L ANOTEHR SESSION !

    public boolean validate(String incomingToken , String seed ) {
        long currentWindow = TOTPEngine.currentWindow();
        long previousWindow = currentWindow - 1;

        String expectedCurrent = engine.generateToken(seed, currentWindow);
        String expectedPrevious = engine.generateToken(seed , previousWindow);

        if(incomingToken.equals(expectedCurrent) || incomingToken.equals(expectedPrevious))
        {
            return true ;
        }

        throw new ExpiredQRException(
                "QR code has expired . the token '" + incomingToken + "'does not match the current or even the previous window ");

    }

}
