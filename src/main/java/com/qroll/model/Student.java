package com.qroll.model;

import java.util.Objects;

public class Student {

    private String studentId; // Matricule
    private String lastName;  // Nom
    private String firstName; // Prénom
    private String section;   // Section
    private String group;     // Groupe
    private String email;     // Email

    public Student() {}

    public Student(String studentId, String lastName, String firstName, String section, String group, String email) {
        this.studentId = studentId;
        this.lastName  = lastName;
        this.firstName = firstName;
        this.section   = section;
        this.group     = group;
        this.email     = email;
    }

    // Legacy constructor for backward compatibility
    public Student(String studentId, String fullName, String group, String email) {
        this.studentId = studentId;
        if (fullName != null) {
            String[] parts = fullName.trim().split("\\s+", 2);
            this.lastName  = parts[0];
            this.firstName = parts.length > 1 ? parts[1] : "";
        } else {
            this.lastName  = "";
            this.firstName = "";
        }
        this.section = "";
        this.group   = group;
        this.email   = email;
    }

    public String getStudentId() { return studentId; }
    public String getLastName()  { return lastName != null ? lastName : ""; }
    public String getFirstName() { return firstName != null ? firstName : ""; }
    public String getSection()   { return section != null ? section : ""; }
    public String getGroup()     { return group != null ? group : ""; }
    public String getEmail()     { return email != null ? email : ""; }

    public String getFullName() {
        String l = getLastName();
        String f = getFirstName();
        if (l.isEmpty()) return f;
        if (f.isEmpty()) return l;
        return l + " " + f;
    }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setLastName(String lastName)   { this.lastName  = lastName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setSection(String section)     { this.section   = section; }
    public void setGroup(String group)         { this.group     = group; }
    public void setEmail(String email)         { this.email     = email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return Objects.equals(studentId, ((Student) o).studentId);
    }

    @Override
    public int hashCode() { return Objects.hash(studentId); }

    @Override
    public String toString() {
        return "Student{matricule='" + studentId + "', nom='" + lastName + "', prenom='" + firstName + "', section='" + section + "', groupe='" + group + "'}";
    }
}
