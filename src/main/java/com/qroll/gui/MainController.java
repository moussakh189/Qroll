package com.qroll.gui;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("QRoll started.");
    }

    public void wireControllers(SessionController sc, DashboardController dc) {
        sc.setDashboardController(dc);
        System.out.println("Controllers wired.");
    }
}
