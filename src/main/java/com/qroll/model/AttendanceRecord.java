package com.qroll.model;

import java.time.LocalDateTime;

public class AttendanceRecord {
    private  int recordId ;
    private Student student;
    private Session session ;
    private LocalDateTime scanTimestamp ;
    private boolean valid ;



    public AttendanceRecord() {}

    public AttendanceRecord(Student student, Session session,
                            LocalDateTime scanTimestamp, boolean valid) {
        this.student       = student;
        this.session       = session;
        this.scanTimestamp = scanTimestamp;
        this.valid         = valid;
    }


    public AttendanceRecord(int recordId, Student student, Session session,
                            LocalDateTime scanTimestamp, boolean valid) {
        this.recordId      = recordId;
        this.student       = student;
        this.session       = session;
        this.scanTimestamp = scanTimestamp;
        this.valid         = valid;
    }



    public int           getRecordId()      { return recordId; }
    public Student       getStudent()       { return student; }
    public Session       getSession()       { return session; }
    public LocalDateTime getScanTimestamp() { return scanTimestamp; }
    public boolean       isValid()          { return valid; }

    public void setRecordId(int recordId)                  { this.recordId      = recordId; }
    public void setStudent(Student student)                { this.student       = student; }
    public void setSession(Session session)                { this.session       = session; }
    public void setScanTimestamp(LocalDateTime scanTimestamp) { this.scanTimestamp = scanTimestamp; }
    public void setValid(boolean valid)                    { this.valid         = valid; }



    @Override
    public String toString() {
        return "AttendanceRecord{id=" + recordId
                + ", student=" + (student != null ? student.getStudentId() : "null")
                + ", session=" + (session != null ? session.getSessionId() : "null")
                + ", time=" + scanTimestamp + ", valid=" + valid + "}";
    }
}




