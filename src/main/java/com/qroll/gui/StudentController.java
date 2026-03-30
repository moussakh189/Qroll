package com.qroll.gui;

import com.qroll.database.StudentRepository;
import com.qroll.model.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

public class StudentController implements Initializable {

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student,String> idCol;
    @FXML private TableColumn<Student,String> nameCol;
    @FXML private TableColumn<Student,String> groupCol;
    @FXML private TableColumn<Student,String> emailCol;

    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    @FXML private Label     importStatusLabel;



    @FXML private javafx.scene.layout.VBox addForm;
    @FXML private TextField newIdField;
    @FXML private TextField newNameField;
    @FXML private TextField newGroupField;
    @FXML private TextField newEmailField;
    @FXML private Label     formErrorLabel;


    private final ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private FilteredList<Student> filteredStudents;
    private final StudentRepository studentRepo     = new StudentRepository();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        loadStudents();

    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs , old , text ) -> {
            filteredStudents.setPredicate(s ->{ if(text == null || text.isEmpty() )return  true;
                String lower = text.toLowerCase();
                return s.getStudentId().toLowerCase().contains(lower)
                        || s.getFullName().toLowerCase().contains(lower)
                        || s.getGroup().toLowerCase().contains(lower)
                        || (s.getEmail() != null && s.getEmail().toLowerCase().contains(lower));
            }
            );
        });
    }

    private void loadStudents() {
        List<Student> students = studentRepo.findAll();
        allStudents.setAll(students);
        updateStatus();
        
    }



    private void setupTable() {
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentId()));
        nameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFullName()));
        groupCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getGroup()));
        emailCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEmail()));


        filteredStudents = new FilteredList<>(allStudents);
        studentTable.setItems(filteredStudents);
        studentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
    }

    @FXML
    private void onAddStudent() {
        addForm.setVisible(true);
        addForm.setManaged(true);
        formErrorLabel.setText("");
        newIdField.clear();
        newNameField.clear();
        newGroupField.clear();
        newEmailField.clear();
        newIdField.requestFocus();
    }

    @FXML
    private void onCancelAdd() {
        addForm.setVisible(false);
        addForm.setManaged(false);
        formErrorLabel.setText("");
    }

    @FXML
    private void onSaveNewStudent() {
        String id = newIdField.getText().trim();
        String name = newNameField.getText().trim();
        String group = newGroupField.getText().trim();
        String email = newEmailField.getText().trim();


        if (id.isEmpty() || name.isEmpty() || group.isEmpty()) {
            formErrorLabel.setText("Student ID, Name and Group are required.");
            return;
        }
        if (studentRepo.findById(id) != null) {
            formErrorLabel.setText("Student ID '" + id + "' already exists.");
            return;
        }

        Student s = new Student(id, name, group, email);
        studentRepo.save(s);
        allStudents.add(s);


        onCancelAdd();
        importStatusLabel.setText("Student '" + name + "' added.");
    }

    @FXML
    private void onDeleteSelected() {
        List<Student> selected = studentTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            importStatusLabel.setText("Select one or more students to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Students");
        confirm.setHeaderText("Delete " + selected.size() + " student(s)?");
        confirm.setContentText("This cannot be undone. Attendance records for these students will remain.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                for (Student s : selected) {
                    studentRepo.delete(s.getStudentId());
                }
                loadStudents();
                importStatusLabel.setText("Deleted " + selected.size() + " student(s).");
            }
        });
    }

    @FXML
    private void onImportCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Students from CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) chooser.setInitialDirectory(desktop);

        File file = chooser.showOpenDialog(null);
        if (file == null) return;

        int imported = 0;
        int skipped  = 0;

        try (BufferedReader br = Files.newBufferedReader(
                file.toPath(), StandardCharsets.UTF_8)) {

            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                // Skip header row if it starts with "student" (case-insensitive)
                if (lineNum == 1 && line.toLowerCase().startsWith("student")) continue;

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.err.println("Skipping malformed row " + lineNum + ": " + line);
                    skipped++;
                    continue;
                }

                String id    = parts[0].trim();
                String name  = parts[1].trim();
                String group = parts[2].trim();
                String email = parts.length >= 4 ? parts[3].trim() : "";

                if (id.isEmpty() || name.isEmpty()) {
                    skipped++;
                    continue;
                }

                // Skip if already exists
                if (studentRepo.findById(id) != null) {
                    System.out.println("Skipping duplicate: " + id);
                    skipped++;
                    continue;
                }

                try {
                    studentRepo.save(new Student(id, name, group, email));
                    imported++;
                } catch (Exception e) {
                    System.err.println("Error saving row " + lineNum + ": " + e.getMessage());
                    skipped++;
                }
            }

        } catch (Exception e) {
            importStatusLabel.setText("Import failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        loadStudents();
        importStatusLabel.setText("Imported: " + imported
                + (skipped > 0 ? "  |  Skipped: " + skipped : ""));
    }
    

    private void updateStatus() {
        int total    = allStudents.size();
        int showing  = filteredStudents.size();
        if (total == showing) {
            statusLabel.setText(total + " student" + (total == 1 ? "" : "s"));
        } else {
            statusLabel.setText("Showing " + showing + " of " + total + " students");
        }
    }


}
