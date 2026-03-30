package com.qroll.server;


import com.qroll.crypto.TOTPValidator;
import com.qroll.database.AttendanceRepository;
import com.qroll.database.StudentRepository;
import com.qroll.exception.DuplicateScanException;
import com.qroll.exception.ExpiredQRException;
import com.qroll.exception.SessionNotActiveException;
import com.qroll.exception.StudentNotFoundException;
import com.qroll.model.AttendanceRecord;
import com.qroll.model.Session;
import com.qroll.model.Student;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@RestController
public class ScanController {
    private  final TOTPValidator validator = new TOTPValidator() ;
    private  final StudentRepository studentRepo = new StudentRepository() ;
    private final AttendanceRepository attendanceRepo = new AttendanceRepository() ;
    private volatile Session activeSession = null ;
    private final RateLimiter          rateLimiter    = new RateLimiter();
    private final DeviceGuard          deviceGuard    = new DeviceGuard();

    private Consumer<AttendanceRecord> onNewScan = null ;

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{1,30}$");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final Pattern SESSION_UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(rateLimiter::pruneStaleEntries, 5, 5, TimeUnit.MINUTES);
    }
    public void setActiveSession(Session session) {
        this.activeSession = session;
        deviceGuard.reset();
    }

    public void clearActiveSession() {
        this.activeSession = null;
        deviceGuard.reset();
    }


    public void setOnNewScan(Consumer<AttendanceRecord> callback) {
        this.onNewScan = callback;
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scan(
            @RequestParam(name = "studentId") String studentId,
            @RequestParam(name = "token")     String token,
            @RequestParam(name = "session")   String session,
            HttpServletRequest httpRequest)
    {
        String clientIp = httpRequest.getRemoteAddr();

        System.out.println("Scan received — studentId=" + studentId
                + ", token=" + token + ", ip=" + clientIp);


        if (!rateLimiter.isAllowed(clientIp)) {
            System.out.println("Rate limit exceeded — ip=" + clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "status",  "error",
                            "message", "Too many attempts from this device. Please wait a moment."
                    ));
        }



        if (studentId == null || studentId.isBlank())
        {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Student ID is required."));
        }

        studentId = studentId.trim().toUpperCase();


        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error",
                            "message", "Invalid student ID format."));
        }
        if (token == null || !TOKEN_PATTERN.matcher(token.trim()).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error",
                            "message", "Invalid token format. Please scan the QR again."));
        }
        if (session == null || !SESSION_UUID_PATTERN.matcher(session.trim()).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error",
                            "message", "Invalid session ID. Please scan the QR again."));
        }

        token   = token.trim();
        session = session.trim();


        if(activeSession == null )
        {
            throw new SessionNotActiveException("NO session is currently active .");
        }

        if (!activeSession.getSessionId().toString().equals(session)) {
            throw new SessionNotActiveException("This QR belongs to a different session.");
        }
        validator.validate(token, activeSession.getTotpSeed());

        Student student = studentRepo.findById(studentId);
        if (student == null) {
            throw new StudentNotFoundException(
                    "Student ID '" + studentId + "' is not registered in the system.");
        }

        if (!deviceGuard.isFirstScan(clientIp)) {
            System.out.println("Device already used this session — ip=" + clientIp);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status",  "error",
                            "message", "This device has already submitted attendance for this session. " +
                                    "Each device can only be used once."
                    )); }




        if (attendanceRepo.hasAlreadyScanned(studentId, activeSession.getSessionId())) {
            throw new DuplicateScanException(
                    "You have already marked your attendance for this session.");
        }

        AttendanceRecord record = new AttendanceRecord(
                student, activeSession, LocalDateTime.now(), true
        );

        attendanceRepo.save(record);
        if (onNewScan != null) {
            onNewScan.accept(record);
        }
        System.out.println("Attendance saved for " + student.getFullName());




        return ResponseEntity.ok(Map.of(
                "status", "present",
                "message", "Attendance marked successfully!",
                "name", student.getFullName()
        ));


    }



    @ExceptionHandler(SessionNotActiveException.class)
    public ResponseEntity<Map<String,String >> handleSessionNotActive(SessionNotActiveException e )
    {
        System.out.println("Rejected :" + e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status" , "error" , "message " , e.getMessage()));

    }

    @ExceptionHandler(ExpiredQRException.class)
    public ResponseEntity<Map<String,String>> handleExpiredQR(ExpiredQRException e )
    {
        System.out.println("Rejected:" + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "QR code expired. Ask professor to show the latest QR."));

    }

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<Map<String , String>> handleStudentNotFound(StudentNotFoundException e )
    {
        System.out.println("Rejeceted:" + e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status" , "error" , "message" , e.getMessage()));
    }

    @ExceptionHandler(DuplicateScanException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateScanException e) {
        System.out.println("Rejected: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", "error", "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        System.err.println("Unexpected error in ScanController: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error",
                        "message", "An unexpected error occurred. Please try again."));
    }

    }







