package com.qroll.database;

import com.qroll.model.Module;
import com.qroll.model.Session;
import com.qroll.model.SessionStatus;
import com.qroll.model.SessionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionRepository {

    private final ModuleRepository moduleRepo = new ModuleRepository();

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }



    public void save(Session s) {
        String sql = """
            INSERT OR REPLACE INTO sessions
                (session_id, module_code, session_type, start_time, end_time, status, totp_seed)
            VALUES (?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, s.getSessionId().toString());
            ps.setString(2, s.getModule().getModuleCode());
            ps.setString(3, s.getType().name());
            ps.setString(4, s.getStartTime() != null ? s.getStartTime().toString() : null);
            ps.setString(5, s.getEndTime()   != null ? s.getEndTime().toString()   : null);
            ps.setString(6, s.getStatus().name());
            ps.setString(7, s.getTotpSeed());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void closeSession(UUID sessionId) {
        String sql = "UPDATE sessions SET status='CLOSED', end_time=? WHERE session_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, sessionId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public Session findById(UUID sessionId) {
        String sql = "SELECT * FROM sessions WHERE session_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, sessionId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Session> findByModule(String moduleCode) {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM sessions WHERE module_code = ? ORDER BY start_time DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, moduleCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Session> findAll() {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT * FROM sessions ORDER BY start_time DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Session mapRow(ResultSet rs) throws SQLException {
        String moduleCode = rs.getString("module_code");
        Module module     = moduleRepo.findByCode(moduleCode);
        if (module == null) {
            // Fallback: create a stub module so nothing blows up
            module = new Module(moduleCode, moduleCode, 0);
        }

        String endTimeStr   = rs.getString("end_time");
        String startTimeStr = rs.getString("start_time");

        return new Session(
                UUID.fromString(rs.getString("session_id")),
                module,
                SessionType.valueOf(rs.getString("session_type")),
                startTimeStr != null ? LocalDateTime.parse(startTimeStr) : null,
                endTimeStr   != null ? LocalDateTime.parse(endTimeStr)   : null,
                SessionStatus.valueOf(rs.getString("status")),
                rs.getString("totp_seed")
        );
    }
}
