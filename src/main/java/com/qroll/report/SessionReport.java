package com.qroll.report;

import com.qroll.database.AttendanceRepository;
import com.qroll.database.StudentRepository;
import com.qroll.model.AttendanceRecord;
import com.qroll.model.Session;
import com.qroll.model.Student;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class SessionReport  implements Reportable {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter T_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Session session;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    public SessionReport(Session session) {
        this.session = session;
        this.attendanceRepository = new AttendanceRepository();
        this.studentRepository = new StudentRepository();
    }

    @Override
    public void export(Path outputPath) throws IOException {
        List<AttendanceRecord> records = attendanceRepository.findBySession(session.getSessionId());
        List<Student> allStudents = studentRepository.findAll();

        // Build set of present student IDs for quick lookup

        Set<String> presentIds = records.stream().map(r -> r.getStudent().getStudentId()).collect(Collectors.toSet());
        List<Student> absentStudents = allStudents.stream().filter(s -> !presentIds.contains(s.getStudentId())).collect(Collectors.toList());


        try (BufferedWriter bw = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {

            bw.write("QRoll Session Attendance Report");
            bw.newLine();
            bw.write("Teacher:," + escape(session.getTeacherName().isEmpty() ? "N/A" : session.getTeacherName()));
            bw.newLine();
            bw.write("Session:," + session.getModule().getModuleCode() + " - " + session.getType().name());
            bw.newLine();
            bw.write("Date:," + (session.getStartTime() != null ? session.getStartTime().format(DT_FMT) : "N/A"));
            bw.newLine();
            bw.write("Status:," + session.getStatus().name());
            bw.newLine();
            bw.newLine();

            bw.write("PRESENT (" + records.size() + " students)");
            bw.newLine();
            bw.write("Matricule,Nom,Prénom,Section,Groupe,Scan Time,Status");
            bw.newLine();
            for (AttendanceRecord r : records) {
                boolean late = r.getScanTimestamp()
                        .isAfter(session.getStartTime().plusMinutes(15));
                Student s = r.getStudent();
                bw.write(
                        escape(s.getStudentId()) + "," +
                                escape(s.getLastName()) + "," +
                                escape(s.getFirstName()) + "," +
                                escape(s.getSection()) + "," +
                                escape(s.getGroup()) + "," +
                                r.getScanTimestamp().format(T_FMT) + "," +
                                (late ? "LATE" : "ON TIME")
                );
                bw.newLine();
            }
            bw.newLine();

            bw.write("ABSENT (" + absentStudents.size() + " students)");
            bw.newLine();
            bw.write("Matricule,Nom,Prénom,Section,Groupe");
            bw.newLine();

            for (Student s : absentStudents) {
                bw.write(
                        escape(s.getStudentId()) + "," +
                                escape(s.getLastName()) + "," +
                                escape(s.getFirstName()) + "," +
                                escape(s.getSection()) + "," +
                                escape(s.getGroup())
                );
                bw.newLine();
            }
            bw.newLine();

            int total = records.size() + absentStudents.size();
            double rate = total == 0 ? 0 : (records.size() * 100.0 / total);
            bw.write("SUMMARY");
            bw.newLine();
            bw.write("Present," + records.size());
            bw.newLine();
            bw.write("Absent," + absentStudents.size());
            bw.newLine();
            bw.write("Total," + total);
            bw.newLine();
            bw.write("Attendance Rate," + String.format("%.1f%%", rate));
            bw.newLine();
        }
    }

    @Override
    public String getDefaultFileName() {
        return "QRoll_Session_"
                + session.getModule().getModuleCode() + "_"
                + session.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + ".csv";
    }


    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }


        return value ;
    }

}
