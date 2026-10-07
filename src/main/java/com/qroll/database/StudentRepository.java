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
        String sql = "INSERT OR REPLACE INTO students (student_id, last_name, first_name, section, group_name, email, full_name) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getLastName());
            ps.setString(3, s.getFirstName());
            ps.setString(4, s.getSection());
            ps.setString(5, s.getGroup());
            ps.setString(6, s.getEmail());
            ps.setString(7, s.getFullName());
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
        String sql = "SELECT * FROM students ORDER BY COALESCE(last_name, full_name), first_name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int importFromCSV(Path file) {
        int count = 0;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                // Skip header row if it starts with matricule/student
                String lower = line.toLowerCase();
                if (lineNum == 1 && (lower.startsWith("matricule") || lower.startsWith("student")))
                    continue;

                String[] parts = line.split("[,;\\t]+");
                if (parts.length < 3) {
                    System.err.println("Skipping malformed row: " + line);
                    continue;
                }

                try {
                    Student s;
                    if (parts.length >= 5) {
                        // Matricule, Nom, Prénom, Section, Groupe (Optionally Email as 6th)
                        String email = parts.length >= 6 ? parts[5].trim() : "";
                        s = new Student(
                                parts[0].trim(),
                                parts[1].trim(),
                                parts[2].trim(),
                                parts[3].trim(),
                                parts[4].trim(),
                                email
                        );
                    } else if (parts.length == 4) {
                        // Matricule, Full Name, Group, Email
                        s = new Student(
                                parts[0].trim(),
                                parts[1].trim(),
                                parts[2].trim(),
                                parts[3].trim()
                        );
                    } else {
                        // Matricule, Nom/Full Name, Group
                        s = new Student(
                                parts[0].trim(),
                                parts[1].trim(),
                                parts[2].trim(),
                                ""
                        );
                    }
                    save(s);
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
        String studentId = rs.getString("student_id");
        String lastName  = rs.getString("last_name");
        String firstName = rs.getString("first_name");
        String section   = rs.getString("section");
        String groupName = rs.getString("group_name");
        String email     = rs.getString("email");
        String fullName  = rs.getString("full_name");

        if (lastName == null && firstName == null && fullName != null) {
            return new Student(studentId, fullName, groupName, email);
        }

        return new Student(
                studentId,
                lastName  != null ? lastName  : "",
                firstName != null ? firstName : "",
                section   != null ? section   : "",
                groupName != null ? groupName : "",
                email     != null ? email     : ""
        );
    }
}
