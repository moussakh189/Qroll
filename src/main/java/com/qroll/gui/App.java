package com.qroll.gui;
import  javafx.application.Application ;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.qroll.database.DatabaseManager;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load() , 1100 , 720 );

        MainController      mc = loader.getController();
        SessionController   sc = (SessionController)   loader.getNamespace().get("sessionController");
        DashboardController dc = (DashboardController) loader.getNamespace().get("dashboardController");

        mc.wireControllers(sc, dc);

        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("QRoll v1.1 - Attendance System");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Graceful shutdown: close DB connection so WAL is flushed cleanly
        DatabaseManager.getInstance().closeConnection();
        System.out.println("QRoll: shutdown complete.");
    }

    public static void main(String[] args)
    {
        launch(args);
    }

}
