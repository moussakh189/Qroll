package com.qroll.report;

import com.qroll.database.AttendanceRepository;
import com.qroll.database.ModuleRepository;
import com.qroll.database.SessionRepository;
import com.qroll.model.AttendanceRecord;
import com.qroll.model.Session;
import com.qroll.model.Student;
import com.qroll.model.Module;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class StudentReport implements Reportable{



    private static final double THRESHOLD = 75.0;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Student student;
    private final AttendanceRepository attendanceRepo;
    private final SessionRepository sessionRepo;
    private final ModuleRepository moduleRepo;

    public StudentReport(Student student) {
        this.student        = student;
        this.attendanceRepo = new AttendanceRepository();
        this.sessionRepo    = new SessionRepository();
        this.moduleRepo     = new ModuleRepository();
    }

    @Override
    public void export(Path outputPath) throws IOException {


        List<AttendanceRecord> allRecords = attendanceRepo.findByStudent(student.getStudentId());
        List<Module> modules    = moduleRepo.findAll();


        try(BufferedWriter bw = Files.newBufferedWriter(outputPath , StandardCharsets.UTF_8))
        {
            bw.write("QRoll Student Attendance Report");
            bw.newLine();
            bw.write("Student ID:," + student.getStudentId());
            bw.newLine();
            bw.write("Name:," + escape(student.getFullName()));
            bw.newLine();
            bw.write("Group:," + student.getGroup());
            bw.newLine();
            bw.write("Email:," + student.getEmail());
            bw.newLine();
            bw.write("Generated:," + LocalDateTime.now().format(DT_FMT));
            bw.newLine();
            bw.newLine();

            bw.write("ATTENDANCE BY MODULE");
            bw.newLine();
            bw.newLine();

            int totalPresent  = 0;
            int totalSessions = 0;

            for(Module module: modules )
            {
                List<Session> moduleSession = sessionRepo.findByModule(module.getModuleCode());
                int  sessionCount = moduleSession.size();
                if(sessionCount == 0 ) continue ;


                List<String> sessionIds = moduleSession.stream()
                        .map(s -> s.getSessionId().toString())
                        .collect(Collectors.toList());

                long attendedCount = allRecords.stream().filter(r -> sessionIds.contains(r.getSession().getSessionId().toString())).count();


                double rate = attendedCount * 100.0 / sessionCount;
                String status = rate >= THRESHOLD ? "OK" : "BELOW " + THRESHOLD + "% THRESHOLD"; bw.write("Module:," + module.getModuleCode() + " - " + module.getModuleName());
                bw.newLine();
                bw.write("Sessions attended:," + attendedCount + " / " + sessionCount);
                bw.newLine();
                bw.write("Attendance rate:," + String.format("%.1f%%", rate));
                bw.newLine();
                bw.write("Status:," + status);
                bw.newLine();
                bw.newLine();

                totalPresent  += attendedCount;
                totalSessions += sessionCount;
            }


            // Overall summary
            bw.write("OVERALL SUMMARY");
            bw.newLine();
            bw.write("Total sessions attended:," + totalPresent + " / " + totalSessions);
            bw.newLine();
            if (totalSessions > 0) {
                double overall = totalPresent * 100.0 / totalSessions;
                bw.write("Overall attendance rate:," + String.format("%.1f%%", overall));
                bw.newLine();
                bw.write("Overall status:," + (overall >= THRESHOLD ? "OK" : "AT RISK"));
                bw.newLine();
            }
            bw.newLine();

            bw.write("DETAILED HISTORY");
            bw.newLine();
            bw.write("Session ID,Module,Type,Session Date,Scan Time");
            bw.newLine();

            for (AttendanceRecord r : allRecords) {
                bw.write(
                        r.getSession().getSessionId().toString().substring(0, 8) + "...," +
                                escape(r.getSession().getModule().getModuleCode()) + "," +
                                r.getSession().getType().name() + "," +
                                r.getSession().getStartTime().format(DT_FMT) + "," +
                                r.getScanTimestamp().format(DT_FMT)
                );
                bw.newLine();
            }



        }











    }

    @Override
    public String getDefaultFileName() {
        return "QRoll_Student_"
                + student.getStudentId() + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + ".csv";
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
