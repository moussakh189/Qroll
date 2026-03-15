package com.qroll.model;

import java.util.Objects;

public class Student {

    private String studentId;
    private String fullName;
    private String group;
    private String email;

    public Student() {}

    public Student(String studentId, String fullName, String group, String email) {
        this.studentId = studentId;
        this.fullName  = fullName;
        this.group     = group;
        this.email     = email;
    }

    public String getStudentId() { return studentId; }
    public String getFullName()  { return fullName; }
    public String getGroup()     { return group; }
    public String getEmail()     { return email; }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setFullName(String fullName)   { this.fullName  = fullName; }
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
        return "Student{id='" + studentId + "', name='" + fullName + "', group='" + group + "', email='" + email + "'}";
    }

}
