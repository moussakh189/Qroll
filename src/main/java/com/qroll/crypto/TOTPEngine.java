package com.qroll.crypto;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TOTPEngine {
    private static final int CODE_DIGITS = 6;
    private  static final int TIME_STEP = 30; //  seconds per window
    private static final String HMAC_ALGO = "HmacSHA1";


    public  String generateToken(String seed , long timwWindow )
    {
        try {
            byte[] keyBytes = seed.getBytes(StandardCharsets.UTF_8);
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timwWindow).array();


            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(keyBytes , HMAC_ALGO));
            byte[] hash = mac.doFinal(timeBytes);  // 20 byte result


            int offset = hash[hash.length - 1 ] & 0x0f ;

            int binary = ((hash[offset] & 0x7f << 24 )) |
                    ((hash[offset + 1 ] & 0xff ) << 16 ) |
                    ((hash[offset + 2 ] & 0xff) << 8 ) |
                    (hash[offset + 3 ] & 0xff );


            int code = binary % (int) Math.pow(10 , CODE_DIGITS );

            return String.format("%0" + CODE_DIGITS + "d" , code );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public  String generateCurrentToken(String seed )
    {
        long window = currentWindow();
        return generateToken(seed , window );
    }

    public static long currentWindow() {
        return System.currentTimeMillis() / 1000 / TIME_STEP ;
    }


    private  static int getTimeStep()
    {
        return TIME_STEP ;
    }
}
