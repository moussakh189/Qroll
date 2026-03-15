package com.qroll;

import com.qroll.database.*;
import com.qroll.model.*;
import com.qroll.model.Module;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Week1Test {

    private static StudentRepository    studentRepo;
    private static SessionRepository    sessionRepo;
    private static AttendanceRepository attendanceRepo;
    private static ModuleRepository     moduleRepo;

    // Shared test data
    private static final String  STUDENT_ID = "STU001";
    private static final UUID    SESSION_ID = UUID.randomUUID();
    private static final String  MODULE_CODE = "ALGO_L3";

    @BeforeAll
    static void setup() {
        // Uses the real qroll.db (creates it if it doesn't exist)
        studentRepo    = new StudentRepository();
        sessionRepo    = new SessionRepository();
        attendanceRepo = new AttendanceRepository();
        moduleRepo     = new ModuleRepository();
    }

    // ── Module ────────────────────────────────────────────────────────────────

    @Test @Order(1)
    void saveAndFindModule() {
        Module m = new Module(MODULE_CODE, "Algorithmics L3", 3);
        moduleRepo.save(m);

        Module found = moduleRepo.findByCode(MODULE_CODE);
        assertNotNull(found, "Module should be found after save");
        assertEquals("Algorithmics L3", found.getModuleName());
        System.out.println("PASS: saveAndFindModule → " + found);
    }

    // ── Student ───────────────────────────────────────────────────────────────

    @Test @Order(2)
    void saveAndFindStudent() {
        Student s = new Student(STUDENT_ID, "Moussa Khenfri", "G1", "moussa@ens.dz");
        studentRepo.save(s);

        Student found = studentRepo.findById(STUDENT_ID);
        assertNotNull(found, "Student should be found after save");
        assertEquals("Moussa Khenfri", found.getFullName());
        assertEquals("G1", found.getGroup());
        System.out.println("PASS: saveAndFindStudent → " + found);
    }

    @Test @Order(3)
    void findAllStudentsNotEmpty() {
        List<Student> all = studentRepo.findAll();
        assertFalse(all.isEmpty(), "findAll should return at least 1 student");
        System.out.println("PASS: findAllStudents → " + all.size() + " students");
    }

    @Test @Order(4)
    void findByIdUnknownReturnsNull() {
        Student notFound = studentRepo.findById("DOESNOTEXIST");
        assertNull(notFound, "Unknown student ID should return null");
        System.out.println("PASS: findByIdUnknownReturnsNull");
    }

    // ── CSV import ────────────────────────────────────────────────────────────

    @Test @Order(5)
    void csvImport() throws IOException {
        Path csv = Files.createTempFile("students_test", ".csv");
        Files.writeString(csv,
                "STU100,Ali Benali,G2,ali@ens.dz\n" +
                        "STU101,Sara Mansour,G1,sara@ens.dz\n" +
                        "BAD_ROW_NO_FIELDS\n" +            // should be skipped, not crash
                        "STU102,Yacine Kaci,G3,yacine@ens.dz\n"
        );

        int imported = studentRepo.importFromCSV(csv);
        assertEquals(3, imported, "Should import 3 valid rows, skip 1 malformed");

        assertNotNull(studentRepo.findById("STU100"));
        assertNotNull(studentRepo.findById("STU101"));
        assertNotNull(studentRepo.findById("STU102"));
        System.out.println("PASS: csvImport → " + imported + " students");
        Files.deleteIfExists(csv);
    }

    // ── Session ───────────────────────────────────────────────────────────────

    @Test @Order(6)
    void saveAndFindSession() {
        Module m = moduleRepo.findByCode(MODULE_CODE);
        assertNotNull(m, "Module must exist before saving session");

        Session s = new Session(
                SESSION_ID, m, SessionType.LECTURE,
                LocalDateTime.now(), null,
                SessionStatus.ACTIVE, UUID.randomUUID().toString()
        );
        sessionRepo.save(s);

        Session found = sessionRepo.findById(SESSION_ID);
        assertNotNull(found, "Session should be found after save");
        assertEquals(SessionStatus.ACTIVE, found.getStatus());
        assertEquals(SessionType.LECTURE, found.getType());
        System.out.println("PASS: saveAndFindSession → " + found);
    }

    @Test @Order(7)
    void closeSession() {
        sessionRepo.closeSession(SESSION_ID);

        Session found = sessionRepo.findById(SESSION_ID);
        assertNotNull(found);
        assertEquals(SessionStatus.CLOSED, found.getStatus(), "Session should be CLOSED");
        assertNotNull(found.getEndTime(), "End time should be set after closing");
        System.out.println("PASS: closeSession → status=" + found.getStatus());
    }

    // ── AttendanceRecord ──────────────────────────────────────────────────────

    @Test @Order(8)
    void hasAlreadyScannedFalseBeforeSave() {
        boolean result = attendanceRepo.hasAlreadyScanned(STUDENT_ID, SESSION_ID);
        assertFalse(result, "Should return false before any record is saved");
        System.out.println("PASS: hasAlreadyScannedFalseBeforeSave");
    }

    @Test @Order(9)
    void saveAttendanceRecord() {
        Student student = studentRepo.findById(STUDENT_ID);
        Session session = sessionRepo.findById(SESSION_ID);
        assertNotNull(student); assertNotNull(session);

        AttendanceRecord record = new AttendanceRecord(student, session, LocalDateTime.now(), true);
        attendanceRepo.save(record);

        List<AttendanceRecord> records = attendanceRepo.findBySession(SESSION_ID);
        assertFalse(records.isEmpty(), "Should have at least 1 record after save");
        assertEquals(STUDENT_ID, records.get(0).getStudent().getStudentId());
        System.out.println("PASS: saveAttendanceRecord → " + records.size() + " record(s)");
    }

    @Test @Order(10)
    void hasAlreadyScannedTrueAfterSave() {
        boolean result = attendanceRepo.hasAlreadyScanned(STUDENT_ID, SESSION_ID);
        assertTrue(result, "Should return true after record is saved");
        System.out.println("PASS: hasAlreadyScannedTrueAfterSave");
    }

    @Test @Order(11)
    void hasAlreadyScannedFalseForDifferentSession() {
        UUID differentSession = UUID.randomUUID();
        boolean result = attendanceRepo.hasAlreadyScanned(STUDENT_ID, differentSession);
        assertFalse(result, "Different session — should return false");
        System.out.println("PASS: hasAlreadyScannedFalseForDifferentSession");
    }

    @Test @Order(12)
    void findByStudent() {
        List<AttendanceRecord> records = attendanceRepo.findByStudent(STUDENT_ID);
        assertFalse(records.isEmpty());
        System.out.println("PASS: findByStudent → " + records.size() + " record(s)");
    }
}
