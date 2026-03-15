package com.qroll.model;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Session {
    private UUID sessionId;
    private Module module ;
    private SessionType type ;
    private LocalDateTime startTime ;
    private LocalDateTime endTime ;
    private SessionStatus status ;
    private String totpSeed ; // ONLY FOR SERVER SIDE
    // TO DO

    public Session() {}
    public Session(UUID sessionId, Module module, SessionType type,
                   LocalDateTime startTime, LocalDateTime endTime,
                   SessionStatus status, String totpSeed) {
        this.sessionId = sessionId;
        this.module    = module;
        this.type      = type;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.status    = status;
        this.totpSeed  = totpSeed;
    }


    public UUID          getSessionId() { return sessionId; }
    public Module        getModule()    { return module; }
    public SessionType   getType()      { return type; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }
    public SessionStatus getStatus()    { return status; }
    public String        getTotpSeed()  { return totpSeed; }

    public void setSessionId(UUID sessionId)          { this.sessionId = sessionId; }
    public void setModule(Module module)              { this.module    = module; }
    public void setType(SessionType type)             { this.type      = type; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime)     { this.endTime   = endTime; }
    public void setStatus(SessionStatus status)       { this.status    = status; }
    public void setTotpSeed(String totpSeed)          { this.totpSeed  = totpSeed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Session)) return false;
        return Objects.equals(sessionId, ((Session) o).sessionId);
    }

    @Override
    public int hashCode() { return Objects.hash(sessionId); }

    @Override
    public String toString() {
        return "Session{id=" + sessionId + ", module=" + (module != null ? module.getModuleCode() : "null")
                + ", type=" + type + ", status=" + status + "}";
    }
}




