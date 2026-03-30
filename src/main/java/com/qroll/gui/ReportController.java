package com.qroll.gui;

import com.qroll.database.SessionRepository;
import com.qroll.database.StudentRepository;
import com.qroll.model.Session;
import com.qroll.model.Student;
import com.qroll.report.ReportGenerator;
import com.qroll.report.SessionReport;
import com.qroll.report.StudentReport;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ReportController implements Initializable {
    @FXML private ComboBox<Session> sessionCombo;
    @FXML private ComboBox<Student> studentCombo;
    @FXML private Label sessionReportStatus;
    @FXML private Label studentReportStatus;

    private final SessionRepository sessionRepository = new SessionRepository();
    private  final StudentRepository studentRepository = new StudentRepository();
    private  final ReportGenerator generator = new ReportGenerator();


    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSessionCombo();
        setupStudentCombo();
        loadSessions();
        loadStudents();
    }

    private void loadSessions() {
        List<Session> sessions = sessionRepository.findAll();
        sessionCombo.getItems().setAll(sessions);
        if (sessions.isEmpty()) {
            sessionReportStatus.setText("No sessions in database yet.");
        }
    }

    private void loadStudents() {
        List<Student> students = studentRepository.findAll();
        studentCombo.getItems().setAll(students);
        if (students.isEmpty()) {
            studentReportStatus.setText("No students in database yet.");
        }
    }


    private void setupSessionCombo() {
        sessionCombo.setCellFactory(lv -> new ListCell<>()
        {
            @Override protected void updateItem(Session s , boolean empty )
            {
                super.updateItem(s , empty);
                if(empty || s == null )
                {
                    setText(null);
                    return;
                }
                setText(s.getModule().getModuleCode()
                        + " — " + s.getType().name()
                        + " — " + s.getStartTime().format(FMT)
                        + " [" + s.getStatus().name() + "]");
            }

        });

        sessionCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Session s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s.getModule().getModuleCode()
                        + " — " + s.getType().name()
                        + " — " + s.getStartTime().format(FMT));
            }
        });
    }

    private void setupStudentCombo() {
        studentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s.getStudentId() + " — " + s.getFullName()
                        + " (" + s.getGroup() + ")");
            }
        });
        studentCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s.getStudentId() + " — " + s.getFullName());
            }
        });
    }

    @FXML
    private void onRefreshSessions() {
        loadSessions();
        sessionReportStatus.setText("Sessions refreshed.");
    }

    @FXML
    private void onRefreshStudents() {
        loadStudents();
        studentReportStatus.setText("Students refreshed.");
    }


    @FXML
    private void onExportSession()
    {
        Session selected = sessionCombo.getValue();
        if (selected == null) {
            sessionReportStatus.setText("Please select a session first.");
            return;
        }

        SessionReport report = new SessionReport(selected);

        File file = showSaveDialog(report.getDefaultFileName());
        if(file == null ) return ;
        try{
            generator.export(report , file.toPath());
            sessionReportStatus.setText("Exported:" + file.getName());
        } catch (Exception e) {
            sessionReportStatus.setText("Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void onExportStudent()
    {
        Student selected = studentCombo.getValue();
        if(selected == null ) {
            studentReportStatus.setText("Please select a student first");
            return ;
        }

        StudentReport report = new StudentReport(selected);
        File file = showSaveDialog(report.getDefaultFileName());
        if(file == null ) return ;

        try {
            generator.export(report , file.toPath());
            studentReportStatus.setText("Exported:" + file.getName());
        } catch (Exception e) {

            studentReportStatus.setText("Export failed: " + e.getMessage());
            e.printStackTrace();

        }


    }

    // helper function

    private File showSaveDialog(String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report");
        chooser.setInitialFileName(defaultFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files" , "*.csv"));
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if(desktop.exists()) chooser.setInitialDirectory(desktop);
        return chooser.showSaveDialog(null);

    }




}
