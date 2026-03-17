package com.qroll.server;

public class ScanRequest {

    private String studentId;
    private String token;
    private String sessionId;

    public ScanRequest() {}

    public String getStudentId() { return studentId; }
    public String getToken()     { return token; }
    public String getSessionId() { return sessionId; }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setToken(String token)         { this.token     = token; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    @Override
    public String toString() {
        return "ScanRequest{studentId='" + studentId + "', token='" + token + "', session='" + sessionId + "'}";
    }
}
