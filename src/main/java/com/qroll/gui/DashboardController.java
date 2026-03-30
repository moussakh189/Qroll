package com.qroll.gui;

import com.qroll.database.StudentRepository;
import com.qroll.model.AttendanceRecord;
import com.qroll.model.Session;
import com.qroll.model.Student;
import com.qroll.server.ScanController;
import com.sun.prism.Presentable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
    @FXML
    private Label presentLabel;
    @FXML
    private Label absentLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label lastScanLabel;
    @FXML
    private Label sessionStatusLabel;
    @FXML
    private TextField searchField;


    @FXML
    private TableView<AttendanceRecord> presentTable;
    @FXML
    private TableColumn<AttendanceRecord, String> idCol;
    @FXML
    private TableColumn<AttendanceRecord, String> nameCol;
    @FXML
    private TableColumn<AttendanceRecord, String> groupCol;
    @FXML
    private TableColumn<AttendanceRecord, String> timeCol;
    @FXML
    private TableView<Student> absentTable;
    @FXML
    private TableColumn<Student, String> absentIdCol;
    @FXML
    private TableColumn<Student, String> absentNameCol;
    @FXML
    private TableColumn<Student, String> absentGroupCol;


    private final ObservableList<AttendanceRecord> allRecords = FXCollections.observableArrayList();
    private final ObservableList<Student> absentStudents = FXCollections.observableArrayList();
    private FilteredList<AttendanceRecord> filteredRecords;


    private final StudentRepository studentRepo = new StudentRepository();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Session activeSession;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        setupPresentTable();
        setupAbsentTable();
        setupSearch();
        updateState();
    }

    private void updateState() {
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs , oldVal , newVal) ->
        {
            filteredRecords.setPredicate(record -> {
                if (newVal == null || newVal.isEmpty()) return true;

                String lower = newVal.toLowerCase();
                return record.getStudent().getFullName().toLowerCase().contains(lower)
                        || record.getStudent().getStudentId().toLowerCase().contains(lower)
                        || record.getStudent().getGroup().toLowerCase().contains(lower);
            });
        }
        );
    }

    private void setupAbsentTable() {

        absentIdCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStudentId()));

        absentNameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getFullName()));

        absentGroupCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getGroup()));

        absentTable.setItems(absentStudents);

    }

    private void setupPresentTable() {
        idCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStudent().getStudentId()));

        nameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStudent().getFullName()));

        groupCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStudent().getGroup()));

        timeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getScanTimestamp().format(TIME_FMT)));


        presentTable.setRowFactory(tv -> new TableRow<AttendanceRecord>()
        {
            @Override
            protected void updateItem(AttendanceRecord attendanceRecord, boolean b) {
                super.updateItem(attendanceRecord, b);
                if(attendanceRecord == null || b  )
                {
                    setStyle("");
                    return;
                }
                if (activeSession != null &&
                        attendanceRecord.getScanTimestamp().isAfter(
                                activeSession.getStartTime().plusMinutes(15))) {
                    setStyle("-fx-background-color: #3d2e00;"); // orange = late
                } else {
                    setStyle("-fx-background-color: #0d2818;"); // green = on time
                }
            }
        });


        filteredRecords = new FilteredList<>(allRecords);
        presentTable.setItems(filteredRecords);


            }




            public void onSessionStarted(Session session , ScanController scanController)
            {
                this.activeSession = session ;
                List<Student> all = studentRepo.findAll() ;

                Platform.runLater(() -> {
                    allRecords.clear();
                    absentStudents.setAll(all);
                    totalLabel.setText(String.valueOf(all.size()));
                    presentLabel.setText("0");
                    absentLabel.setText(String.valueOf(all.size()));
                    lastScanLabel.setText("—");
                    sessionStatusLabel.setText("Session active");
                    searchField.clear();
                });

                scanController.setOnNewScan(record ->
                        Platform.runLater(()-> onNewScanReceived(record)));


                System.out.println("Dashboard: listening for scans on session " + session.getSessionId());
            }


    public void onSessionEnded() {
        this.activeSession = null;
        Platform.runLater(() -> {
            sessionStatusLabel.setText("No active session");

            allRecords.clear();
            absentStudents.clear();
            presentLabel.setText("0");
            absentLabel.setText("0");
            totalLabel.setText("0");
            lastScanLabel.setText("—");
        });
    }

    private void onNewScanReceived(AttendanceRecord record) {
        allRecords.add(record);

        absentStudents.removeIf(s ->
                s.getStudentId().equals(record.getStudent().getStudentId()));

        updateStats();
        lastScanLabel.setText(record.getScanTimestamp().format(TIME_FMT));

        System.out.println("Dashboard updated: " + record.getStudent().getFullName() + " is present");
    }

    private void updateStats() {
        int present = allRecords.size();
        int absent  = absentStudents.size();
        int total   = present + absent;

        presentLabel.setText(String.valueOf(present));
        absentLabel.setText(String.valueOf(absent));
        totalLabel.setText(String.valueOf(total));
    }
}




