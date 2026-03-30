package com.qroll;


import com.qroll.crypto.TOTPEngine;
import com.qroll.crypto.TOTPValidator;
import com.qroll.database.AttendanceRepository;
import com.qroll.database.ModuleRepository;
import com.qroll.database.SessionRepository;
import com.qroll.database.StudentRepository;
import com.qroll.exception.ExpiredQRException;
import com.qroll.model.Module;
import com.qroll.server.DeviceGuard;
import com.qroll.server.RateLimiter;
import org.junit.jupiter.api.*;
import com.qroll.model.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QRollTests {

    @Nested
    @DisplayName("TOTPEngine")
    class TOTPEngineTests { private final TOTPEngine engine = new TOTPEngine();
        private final String     seed   = "qroll-test-seed-2026";

        @Test
        @DisplayName("Token is exactly 6 digits")
        void tokenIsExactlySixDigits() {
            String token = engine.generateCurrentToken(seed);
            assertNotNull(token);
            assertEquals(6, token.length());
            assertTrue(token.matches("\\d{6}"));
        }

        @Test
        @DisplayName("Same seed + same window always gives same token")
        void deterministicForSameWindow() {
            long w = TOTPEngine.currentWindow();
            assertEquals(engine.generateToken(seed, w), engine.generateToken(seed, w));
        }

        @Test
        @DisplayName("Different windows produce different tokens")
        void differentWindowsDifferentTokens() {
            long w = TOTPEngine.currentWindow();
            assertNotEquals(engine.generateToken(seed, w), engine.generateToken(seed, w + 1));
        }

        @Test
        @DisplayName("Different seeds produce different tokens")
        void differentSeedsDifferentTokens() {
            long w = TOTPEngine.currentWindow();
            assertNotEquals(engine.generateToken("seed-A", w), engine.generateToken("seed-B", w));
        }

        @Test
        @DisplayName("Token is always zero-padded to 6 chars")
        void tokenAlwaysZeroPadded() {
            for (int i = 0; i < 50; i++) {
                String t = engine.generateToken("seed-" + i, i * 1000L);
                assertEquals(6, t.length(), "Token '" + t + "' is not 6 chars");
            }
        }
    }



    @Nested
    @DisplayName("TOTPValidator")
    class TOTPValidatorTests {

        private final TOTPEngine    engine    = new TOTPEngine();
        private final TOTPValidator validator = new TOTPValidator();
        private final String        seed      = "validator-test-seed";

        @Test
        @DisplayName("Current window token is accepted")
        void currentWindowAccepted() {
            String token = engine.generateToken(seed, TOTPEngine.currentWindow());
            assertTrue(validator.validate(token, seed));
        }

        @Test
        @DisplayName("Previous window token is accepted (30s grace period)")
        void previousWindowAccepted() {
            String token = engine.generateToken(seed, TOTPEngine.currentWindow() - 1);
            assertTrue(validator.validate(token, seed));
        }

        @Test
        @DisplayName("Token from 2 windows ago is rejected")
        void twoWindowsAgoRejected() {
            String old = engine.generateToken(seed, TOTPEngine.currentWindow() - 2);
            assertThrows(ExpiredQRException.class, () -> validator.validate(old, seed));
        }

        @Test
        @DisplayName("Completely wrong token is rejected")
        void wrongTokenRejected() {
            assertThrows(ExpiredQRException.class, () -> validator.validate("000000", seed));
        }

        @Test
        @DisplayName("Token from seed-A does not validate against seed-B")
        void tokenDoesNotCrossSeeds() {
            String token = engine.generateToken("seed-A", TOTPEngine.currentWindow());
            assertThrows(ExpiredQRException.class, () -> validator.validate(token, "seed-B"));
        }
    }
    @Nested
    @DisplayName("AttendanceRepository")
    class AttendanceRepositoryTests {

        private final StudentRepository studentRepo    = new StudentRepository();
        private final SessionRepository sessionRepo    = new SessionRepository();
        private final ModuleRepository moduleRepo     = new ModuleRepository();
        private final AttendanceRepository attendanceRepo = new AttendanceRepository();

        private Student student;
        private Session session;

        @BeforeEach
        void setup() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            student = new Student("TST-" + uid, "Test Student", "G0", "test@enscs.edu.dz");
            studentRepo.save(student);

            Module module = new Module("TST_MOD", "Test Module", 1);
            moduleRepo.save(module);

            session = new Session(UUID.randomUUID(), module, SessionType.LECTURE,
                    LocalDateTime.now(), null, SessionStatus.ACTIVE, "seed-" + uid);
            sessionRepo.save(session);
        }

        @Test
        @DisplayName("Returns false before any scan")
        void falseBeforeAnyScan() {
            assertFalse(attendanceRepo.hasAlreadyScanned(
                    student.getStudentId(), session.getSessionId()));
        }

        @Test
        @DisplayName("Returns true after saving a record")
        void trueAfterScan() {
            attendanceRepo.save(new AttendanceRecord(
                    student, session, LocalDateTime.now(), true));
            assertTrue(attendanceRepo.hasAlreadyScanned(
                    student.getStudentId(), session.getSessionId()));
        }

        @Test
        @DisplayName("Is session-specific — scanned session1 ≠ scanned session2")
        void sessionSpecific() {
            attendanceRepo.save(new AttendanceRecord(
                    student, session, LocalDateTime.now(), true));

            Session session2 = new Session(UUID.randomUUID(), session.getModule(),
                    SessionType.LAB, LocalDateTime.now(), null,
                    SessionStatus.ACTIVE, "another-seed");
            sessionRepo.save(session2);

            assertFalse(attendanceRepo.hasAlreadyScanned(
                    student.getStudentId(), session2.getSessionId()));
        }
    }


    @Nested
    @DisplayName("DeviceGuard — one scan per device per session")
    class DeviceGuardTests {

        private DeviceGuard guard;

        @BeforeEach
        void fresh() { guard = new DeviceGuard(); }

        @Test
        @DisplayName("First scan from a device is allowed")
        void firstScanAllowed() {
            assertTrue(guard.isFirstScan("192.168.1.5"),
                    "First scan from a device must be allowed");
        }

        @Test
        @DisplayName("Second scan from same device is rejected")
        void secondScanRejected() {
            guard.isFirstScan("192.168.1.5");
            assertFalse(guard.isFirstScan("192.168.1.5"),
                    "Second scan from same device must be rejected");
        }

        @Test
        @DisplayName("Different devices are independent")
        void differentDevicesIndependent() {
            guard.isFirstScan("192.168.1.5");
            assertTrue(guard.isFirstScan("192.168.1.6"),
                    "A different device must still be allowed");
        }

        @Test
        @DisplayName("Third attempt from same device still rejected")
        void thirdAttemptStillRejected() {
            guard.isFirstScan("10.0.0.1");
            guard.isFirstScan("10.0.0.1");
            assertFalse(guard.isFirstScan("10.0.0.1"),
                    "Third attempt must also be rejected");
        }

        @Test
        @DisplayName("reset() clears all devices — device can scan again in new session")
        void resetClearsAll() {
            guard.isFirstScan("192.168.1.5"); // device scans session 1
            guard.reset();                    // new session starts
            assertTrue(guard.isFirstScan("192.168.1.5"),
                    "After reset, the same device must be allowed again");
        }

        @Test
        @DisplayName("scannedCount() increments correctly")
        void scannedCountCorrect() {
            assertEquals(0, guard.scannedCount());
            guard.isFirstScan("192.168.1.1");
            guard.isFirstScan("192.168.1.2");
            guard.isFirstScan("192.168.1.1"); // duplicate — not counted
            assertEquals(2, guard.scannedCount());
        }
    }
    @Nested
    @DisplayName("RateLimiter — brute-force protection")
    class RateLimiterTests {

        private RateLimiter limiter;

        @BeforeEach
        void fresh() { limiter = new RateLimiter(); }

        @Test
        @DisplayName("First 10 requests from same IP are allowed")
        void firstTenAllowed() {
            for (int i = 1; i <= 10; i++) {
                assertTrue(limiter.isAllowed("192.168.1.10"),
                        "Request #" + i + " should be allowed");
            }
        }

        @Test
        @DisplayName("11th request from same IP is blocked")
        void eleventhBlocked() {
            for (int i = 0; i < 10; i++) limiter.isAllowed("192.168.1.20");
            assertFalse(limiter.isAllowed("192.168.1.20"),
                    "The 11th request must be blocked");
        }

        @Test
        @DisplayName("Different IPs have independent counters")
        void independentCounters() {
            for (int i = 0; i < 10; i++) limiter.isAllowed("10.0.0.1");
            assertFalse(limiter.isAllowed("10.0.0.1"), "IP-A should be blocked");
            assertTrue(limiter.isAllowed("10.0.0.2"),  "IP-B should still be allowed");
        }
    }




    }

