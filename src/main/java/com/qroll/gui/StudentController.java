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
    @FXML private TableColumn<Student,String> lastNameCol;
    @FXML private TableColumn<Student,String> firstNameCol;
    @FXML private TableColumn<Student,String> sectionCol;
    @FXML private TableColumn<Student,String> groupCol;
    @FXML private TableColumn<Student,String> emailCol;

    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    @FXML private Label importStatusLabel;

    @FXML private javafx.scene.layout.VBox addForm;
    @FXML private TextField newIdField;
    @FXML private TextField newLastNameField;
    @FXML private TextField newFirstNameField;
    @FXML private TextField newSectionField;
    @FXML private TextField newGroupField;
    @FXML private TextField newEmailField;
    @FXML private Label     formErrorLabel;

    private final ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private FilteredList<Student> filteredStudents;
    private final ObservableList<Student> currentPageStudents = FXCollections.observableArrayList();
    private final StudentRepository studentRepo     = new StudentRepository();

    private int rowsPerPage = 50;
    private int currentPage = 0;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        loadStudents();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, text) -> {
            filteredStudents.setPredicate(s -> {
                if (text == null || text.isEmpty()) return true;
                String lower = text.toLowerCase();
                return s.getStudentId().toLowerCase().contains(lower)
                        || s.getLastName().toLowerCase().contains(lower)
                        || s.getFirstName().toLowerCase().contains(lower)
                        || s.getSection().toLowerCase().contains(lower)
                        || s.getGroup().toLowerCase().contains(lower)
                        || s.getEmail().toLowerCase().contains(lower);
            });
            currentPage = 0;
        });
    }

    private void loadStudents() {
        List<Student> students = studentRepo.findAll();
        allStudents.setAll(students);
    }

    private void setupTable() {
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentId()));
        lastNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLastName()));
        firstNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFirstName()));
        sectionCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSection()));
        groupCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGroup()));
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));

        filteredStudents = new FilteredList<>(allStudents);
        filteredStudents.addListener((javafx.collections.ListChangeListener.Change<? extends Student> c) -> {
            updatePage();
        });
        
        studentTable.setItems(currentPageStudents);
        studentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void updatePage() {
        if (filteredStudents == null) return;
        
        int totalSize = filteredStudents.size();
        int totalPages = (int) Math.ceil((double) totalSize / rowsPerPage);
        if (totalPages == 0) totalPages = 1;
        
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalSize);

        currentPageStudents.setAll(filteredStudents.subList(fromIndex, toIndex));
        
        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
        
        updateStatus();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePage();
        }
    }

    @FXML
    private void onNextPage() {
        int totalPages = (int) Math.ceil((double) filteredStudents.size() / rowsPerPage);
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePage();
        }
    }

    @FXML
    private void onAddStudent() {
        addForm.setVisible(true);
        addForm.setManaged(true);
        formErrorLabel.setText("");
        newIdField.clear();
        newLastNameField.clear();
        newFirstNameField.clear();
        newSectionField.clear();
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
        String id        = newIdField.getText().trim();
        String lastName  = newLastNameField.getText().trim();
        String firstName = newFirstNameField.getText().trim();
        String section   = newSectionField.getText().trim();
        String group     = newGroupField.getText().trim();
        String email     = newEmailField.getText().trim();

        if (id.isEmpty() || lastName.isEmpty() || firstName.isEmpty()) {
            formErrorLabel.setText("Matricule, Nom and Prénom are required.");
            return;
        }
        if (studentRepo.findById(id) != null) {
            formErrorLabel.setText("Student with Matricule '" + id + "' already exists.");
            return;
        }

        Student s = new Student(id, lastName, firstName, section, group, email);
        studentRepo.save(s);
        allStudents.add(s);

        onCancelAdd();
        importStatusLabel.setText("Student '" + s.getFullName() + "' added.");
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

        int imported = studentRepo.importFromCSV(file.toPath());
        loadStudents();
        importStatusLabel.setText("Imported " + imported + " student(s) from CSV.");
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
