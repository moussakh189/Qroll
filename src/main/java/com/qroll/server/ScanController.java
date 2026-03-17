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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Consumer;

@RestController
public class ScanController {
    private  final TOTPValidator validator = new TOTPValidator() ;
    private  final StudentRepository studentRepo = new StudentRepository() ;
    private final AttendanceRepository attendanceRepo = new AttendanceRepository() ;
    private volatile Session activeSession = null ;

    private Consumer<AttendanceRecord> onNewScan = null ;

    public void setActiveSession(Session session) {
        this.activeSession = session;
    }

    public void clearActiveSession() {
        this.activeSession = null;
    }


    public void setOnNewScan(Consumer<AttendanceRecord> callback) {
        this.onNewScan = callback;
    }

    @PatchMapping("/scan")
    public ResponseEntity<Map<String , String >> scan(
            @RequestParam String studentId,
            @RequestParam String token ,
            @RequestParam String session ,
            HttpServletRequest httpRequest
    )
    {
        String clientIp = httpRequest.getRemoteAddr();

        System.out.println("Scan received — studentId=" + studentId
                + ", token=" + token + ", ip=" + clientIp);

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

        if (attendanceRepo.hasAlreadyScanned(studentId, activeSession.getSessionId())) {
            throw new DuplicateScanException(
                    "You have already marked your attendance for this session.");
        }

        AttendanceRecord record = new AttendanceRecord(
                student, activeSession, LocalDateTime.now(), true
        );

        attendanceRepo.save(record);
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

    }







