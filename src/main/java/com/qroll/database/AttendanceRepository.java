package com.qroll.database;

import com.qroll.model.AttendanceRecord;
import com.qroll.model.Session;
import com.qroll.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttendanceRepository {
    private final StudentRepository studentRepo = new StudentRepository();
    private final SessionRepository sessionRepository = new SessionRepository();

    private Connection conn()
    {
        return DatabaseManager.getInstance().getConnection();
    }


    public void save(AttendanceRecord r )
    {
        String sql  = """
            INSERT INTO attendance_records (student_id, session_id, scan_time, valid)
            VALUES (?,?,?,?)
        """;

        try(PreparedStatement ps = conn().prepareStatement(sql))
        {
            ps.setString(1 , r.getStudent().getStudentId()  );
            ps.setString(2, r.getSession().getSessionId().toString());
            ps.setString(3, r.getScanTimestamp().toString());
            ps.setInt(4 , r.isValid() ? 1 : 0 );
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasAlreadyScanned(String studentId , UUID sessionId )
    {
        String sql = "SELECT 1 FROM attendance_records WHERE student_id =? AND session_id =?";
        try(PreparedStatement ps = conn().prepareStatement(sql))
        {
            ps.setString(1 , studentId);
            ps.setString(2 , sessionId.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public List<AttendanceRecord> findBySession(UUID sessionId)
    {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance_records WHERE session_id=? ORDER BY scan_time";
        try(PreparedStatement ps = conn().prepareStatement(sql))
        {
            ps.setString(1 , sessionId.toString());
            ResultSet rs = ps.executeQuery() ;
            while(rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list ;
    }

    public List<AttendanceRecord> findByStudent(String studentId)
    {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance_records WHERE student_id=? ORDER BY scan_time DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  list ;
    }

    public double getAttendanceRate(String studentId , String moduleCode)
    {
        String sql = """
            SELECT
                COUNT(ar.record_id) * 100.0 /
                NULLIF(
                    (SELECT COUNT(*) FROM sessions
                     WHERE module_code = ? AND status = 'CLOSED'),
                    0
                ) AS rate
            FROM attendance_records ar
            JOIN sessions s ON ar.session_id = s.session_id
            WHERE ar.student_id = ? AND s.module_code = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, moduleCode);
            ps.setString(2, studentId);
            ps.setString(3, moduleCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double rate = rs.getDouble("rate");
                return rs.wasNull() ? 0.0 : rate;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }




    private AttendanceRecord mapRow(ResultSet rs) throws SQLException {
        String studentId = rs.getString("student_id");
        String sessionId = rs.getString("session_id");
        Student student = studentRepo.findById(studentId);
        Session session = sessionRepository.findById(UUID.fromString(sessionId));


        return new AttendanceRecord(rs.getInt("record_id") , student , session , LocalDateTime.parse(rs.getString("scan_time")) , rs.getInt("valid") == 1 );

    }
}






