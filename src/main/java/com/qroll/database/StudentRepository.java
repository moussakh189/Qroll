package com.qroll.database;

import com.qroll.model.Student;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentRepository {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }


    public void save(Student s) {
        String sql = "INSERT OR REPLACE INTO students (student_id, full_name, group_name, email) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getFullName());
            ps.setString(3, s.getGroup());
            ps.setString(4, s.getEmail());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Student findById(String id) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Student> findAll() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY full_name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }



    public int importFromCSV(Path file )
    {
        int count = 0 ;
        try(BufferedReader br = Files.newBufferedReader(file)) {
            String line ;
            while((line = br.readLine()) != null ) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split(",");
                if (parts.length < 4) {
                    System.err.println("Skipping malformed row: " + line);
                    continue;
                }
                try {
                    save(new Student(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim()
                    ));
                    count++;

                } catch (Exception e) {
                    System.err.println("Skipping row (error): " + line + " — " + e.getMessage());
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("StudentRepository: imported " + count + " students");
        return count;
    }




    public void delete(String studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
                rs.getString("student_id"),
                rs.getString("full_name"),
                rs.getString("group_name"),
                rs.getString("email")
        );
    }
}
