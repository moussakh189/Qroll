package com.qroll.crypto;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;


public class QRCodeGenerator {
    private final QRCodeWriter writer = new QRCodeWriter() ;


    public BufferedImage generate(String content , int size )
    {
        try{
            BitMatrix matrix = writer.encode(content , BarcodeFormat.QR_CODE , size , size );
                    return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
        throw new RuntimeException("QR code generation failed for content: " + content, e);
    }
    }


    public static String buildScanUrl(String serverIp , int port , String token , String sessionId)
    {
        return "http://" + serverIp + ":" + port
                + "/?token=" + token
                + "&session=" + sessionId;
    }








}
