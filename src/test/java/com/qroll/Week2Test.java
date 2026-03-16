package com.qroll;

import com.qroll.crypto.QRCodeGenerator;
import com.qroll.crypto.TOTPEngine;
import com.qroll.crypto.TOTPValidator;
import com.qroll.exception.ExpiredQRException;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Week2Test {

    private static TOTPEngine    engine;
    private static TOTPValidator validator;
    private static final String  SEED = "qroll-test-seed-moussa-2026";

    @BeforeAll
    static void setup() {
        engine    = new TOTPEngine();
        validator = new TOTPValidator();
    }


    @Test @Order(1)
    void tokenIs6Digits() {
        String token = engine.generateCurrentToken(SEED);
        assertNotNull(token);
        assertEquals(6, token.length(), "Token must be exactly 6 digits");
        assertTrue(token.matches("\\d{6}"), "Token must be all digits");
        System.out.println("PASS: tokenIs6Digits → token = " + token);
    }

    @Test @Order(2)
    void sameWindowSameToken() {
        long window = TOTPEngine.currentWindow();
        String t1 = engine.generateToken(SEED, window);
        String t2 = engine.generateToken(SEED, window);
        assertEquals(t1, t2, "Same seed + same window must always produce the same token");
        System.out.println("PASS: sameWindowSameToken → " + t1 + " == " + t2);
    }

    @Test @Order(3)
    void differentWindowDifferentToken() {
        long window = TOTPEngine.currentWindow();
        String current  = engine.generateToken(SEED, window);
        String old      = engine.generateToken(SEED, window - 5);
        assertNotEquals(current, old, "Different windows should produce different tokens");
        System.out.println("PASS: differentWindowDifferentToken → " + current + " != " + old);
    }

    @Test @Order(4)
    void differentSeedDifferentToken() {
        long window = TOTPEngine.currentWindow();
        String t1 = engine.generateToken("seed-one", window);
        String t2 = engine.generateToken("seed-two", window);
        assertNotEquals(t1, t2, "Different seeds must produce different tokens");
        System.out.println("PASS: differentSeedDifferentToken");
    }

    @Test @Order(5)
    void zeroPaddedToken() {

        for (int i = 0; i < 100; i++) {
            String token = engine.generateToken(SEED, i);
            assertEquals(6, token.length(), "Token must always be 6 chars (zero-padded): " + token);
        }
        System.out.println("PASS: zeroPaddedToken → all 100 windows produced 6-char tokens");
    }


    @Test @Order(6)
    void validateCurrentWindowAccepted() {
        long window = TOTPEngine.currentWindow();
        String token = engine.generateToken(SEED, window);
        assertTrue(validator.validate(token, SEED), "Current window token must be accepted");
        System.out.println("PASS: validateCurrentWindowAccepted → token " + token + " accepted");
    }

    @Test @Order(7)
    void validatePreviousWindowAccepted() {

        long window = TOTPEngine.currentWindow();
        String previousToken = engine.generateToken(SEED, window - 1);
        assertTrue(validator.validate(previousToken, SEED), "Previous window token must be accepted");
        System.out.println("PASS: validatePreviousWindowAccepted → previous token accepted");
    }

    @Test @Order(8)
    void validateExpiredTokenThrows() {
        long window = TOTPEngine.currentWindow();

        String oldToken = engine.generateToken(SEED, window - 2);
        assertThrows(ExpiredQRException.class,
                () -> validator.validate(oldToken, SEED),
                "Token older than 2 windows must throw ExpiredQRException"
        );
        System.out.println("PASS: validateExpiredTokenThrows → old token correctly rejected");
    }

    @Test @Order(9)
    void validateWrongTokenThrows() {
        assertThrows(ExpiredQRException.class,
                () -> validator.validate("000000", SEED),
                "Completely wrong token must throw ExpiredQRException"
        );
        System.out.println("PASS: validateWrongTokenThrows");
    }

    @Test @Order(10)
    void validateWrongSeedThrows() {
        long window = TOTPEngine.currentWindow();
        String tokenForSeedA = engine.generateToken("seed-A", window);
        assertThrows(ExpiredQRException.class,
                () -> validator.validate(tokenForSeedA, "seed-B"),
                "Token generated with seed-A must fail validation against seed-B"
        );
        System.out.println("PASS: validateWrongSeedThrows");
    }



    @Test @Order(11)
    void generateQRNotNull() {
        QRCodeGenerator qrGen = new QRCodeGenerator();
        String url = QRCodeGenerator.buildScanUrl("192.168.1.5", 8080,
                engine.generateCurrentToken(SEED), UUID.randomUUID().toString());

        BufferedImage image = qrGen.generate(url, 300);
        assertNotNull(image, "Generated QR image must not be null");
        assertEquals(300, image.getWidth(),  "QR width must match requested size");
        assertEquals(300, image.getHeight(), "QR height must match requested size");
        System.out.println("PASS: generateQRNotNull → 300x300 image generated");
    }

    @Test @Order(12)
    void generateQRSavesToFile() throws IOException {
        QRCodeGenerator qrGen = new QRCodeGenerator();
        String token = engine.generateCurrentToken(SEED);
        String sessionId = UUID.randomUUID().toString();
        String url = QRCodeGenerator.buildScanUrl("192.168.1.5", 8080, token, sessionId);

        BufferedImage image = qrGen.generate(url, 300);
        File output = new File("test_qr.png");
        ImageIO.write(image, "PNG", output);

        assertTrue(output.exists(), "test_qr.png must be created");
        assertTrue(output.length() > 0, "test_qr.png must not be empty");
        System.out.println("PASS: generateQRSavesToFile → saved to " + output.getAbsolutePath());
        System.out.println("      URL encoded: " + url);
        System.out.println("      Scan test_qr.png with your phone — browser should open that URL");
    }

    @Test @Order(13)
    void buildScanUrlFormat() {
        String url = QRCodeGenerator.buildScanUrl("192.168.1.100", 8080, "123456", "my-session-id");
        assertEquals("http://192.168.1.100:8080/?token=123456&session=my-session-id", url);
        System.out.println("PASS: buildScanUrlFormat → " + url);
    }
}
