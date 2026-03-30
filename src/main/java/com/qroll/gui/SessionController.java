package com.qroll.gui;
import com.qroll.crypto.QRCodeGenerator;
import com.qroll.crypto.TOTPEngine;
import com.qroll.database.ModuleRepository;
import com.qroll.database.SessionRepository;
import com.qroll.model.Module;
import com.qroll.model.Session;
import com.qroll.model.SessionStatus;
import com.qroll.model.SessionType;
import com.qroll.server.AttendanceServer;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.net.InetAddress;
import  java.net.Inet4Address;
import java.net.URL;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;





public class SessionController implements Initializable {

    @FXML
    private ComboBox<Module> moduleCombo;
    @FXML
    private RadioButton lectureRadio;
    @FXML
    private RadioButton labRadio;
    @FXML
    private RadioButton tutorialRadio;
    @FXML
    private ToggleGroup typeGroup;
    @FXML
    private Button startBtn;
    @FXML
    private Button endBtn;
    @FXML
    private ImageView qrImageView;
    @FXML
    private Label timerLabel;
    @FXML
    private Label statusLabel;

    private final ModuleRepository moduleRepo = new ModuleRepository();
    private final SessionRepository sessionRepo = new SessionRepository();
    private final TOTPEngine totpEngine = new TOTPEngine();
    private final QRCodeGenerator qrGenerator = new QRCodeGenerator();
    private final AttendanceServer server = new AttendanceServer();

    private Session currentSession;
    private ScheduledExecutorService qrScheduler;
    private ScheduledExecutorService timerScheduler;
    private WritableImage currentQrImage;
    private BooleanBinding startBtnBinding;
    private DashboardController dashboardController;
    private String cachedLocalIp;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadModules();
        endBtn.setDisable(true);
        statusLabel.setText("No active session");
        applyStartBinding();
        qrImageView.setOnMouseClicked(this::openFullscreen);
    }

    private void applyStartBinding() {
        startBtnBinding = moduleCombo.valueProperty().isNull()
                .or(typeGroup.selectedToggleProperty().isNull());
        startBtn.disableProperty().bind(startBtnBinding);
    }


    @FXML
    private void onStartClicked() {
        Module module = moduleCombo.getValue();
        SessionType type = getSelectedType();
        if (module == null || type == null) return;
        cachedLocalIp = getLocalIp();
        System.out.println("SessionController: LAN IP detected as " + cachedLocalIp);
        currentSession = new Session(
                UUID.randomUUID(), module, type,
                LocalDateTime.now(), null,
                SessionStatus.ACTIVE, UUID.randomUUID().toString()
        );
        sessionRepo.save(currentSession);
        refreshQR();

        qrScheduler = Executors.newSingleThreadScheduledExecutor();
        qrScheduler.scheduleAtFixedRate(this::refreshQR, 30, 30, TimeUnit.SECONDS);

        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        timerScheduler.scheduleAtFixedRate(() -> {
            if (currentSession == null) return;
            long secs = Duration.between(
                    currentSession.getStartTime(), LocalDateTime.now()).getSeconds();
            String time = String.format("%02d:%02d:%02d",
                    secs / 3600, (secs % 3600) / 60, secs % 60);
            Platform.runLater(() -> timerLabel.setText(time));
        }, 0, 1, TimeUnit.SECONDS);

        Session sessionRef = currentSession;
        new Thread(() -> {
            server.start(sessionRef);

            while (!server.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            if (dashboardController != null && server.getScanController() != null) {
                dashboardController.onSessionStarted(sessionRef, server.getScanController());
            }
        }).start();


        startBtn.disableProperty().unbind();
        startBtn.setDisable(true);

        endBtn.setDisable(false);
        moduleCombo.setDisable(true);
        lectureRadio.setDisable(true);
        labRadio.setDisable(true);
        tutorialRadio.setDisable(true);
        statusLabel.setText("Session active — " + module.getModuleCode()
                + " " + type.name() + "  |  IP: " + cachedLocalIp + ":8080");
        System.out.println("Session started: " + currentSession.getSessionId());
    }

    @FXML
    private void onEndClicked() {
        server.stop();
        if (currentSession != null) sessionRepo.closeSession(currentSession.getSessionId());

        if (qrScheduler != null) {
            qrScheduler.shutdownNow();
            qrScheduler = null;
        }
        if (timerScheduler != null) {
            timerScheduler.shutdownNow();
            timerScheduler = null;
        }
        if (dashboardController != null) dashboardController.onSessionEnded();


        currentSession = null;
        currentQrImage = null;
        cachedLocalIp = null;

        Platform.runLater(() -> {
            qrImageView.setImage(null);
            timerLabel.setText("00:00:00");
            statusLabel.setText("No active session");
            endBtn.setDisable(true);
            moduleCombo.setDisable(false);
            lectureRadio.setDisable(false);
            labRadio.setDisable(false);
            tutorialRadio.setDisable(false);
            applyStartBinding();
        });
        System.out.println("Session ended.");
    }

    private void refreshQR() {
        if (currentSession == null) return;
        try {
            long window = TOTPEngine.currentWindow();
            String token = totpEngine.generateToken(currentSession.getTotpSeed(), window);
            String ip = cachedLocalIp != null ? cachedLocalIp : getLocalIp();
            String url = QRCodeGenerator.buildScanUrl(
                    ip, 8080, token, currentSession.getSessionId().toString());
            BufferedImage buffered = qrGenerator.generate(url, 320);
            WritableImage fxImage = SwingFXUtils.toFXImage(buffered, null);
            currentQrImage = fxImage;
            Platform.runLater(() -> qrImageView.setImage(fxImage));
            System.out.println("QR refreshed — token: " + token + " | url: " + url);
        } catch (Exception e) {
            System.err.println("QR refresh failed: " + e.getMessage());
        }
    }

    private void openFullscreen(MouseEvent event) {
        if (currentQrImage == null) return;
        ImageView fullQr = new ImageView(currentQrImage);
        fullQr.setPreserveRatio(true);
        fullQr.setFitWidth(600);
        fullQr.setFitHeight(600);
        StackPane pane = new StackPane(fullQr);
        pane.setStyle("-fx-background-color: white;");
        Stage fullStage = new Stage();
        fullStage.setTitle("QRoll — Scan QR Code");
        fullStage.setScene(new Scene(pane, 650, 650));
        fullStage.setFullScreen(true);
        fullStage.show();
    }

    private SessionType getSelectedType() {
        if (lectureRadio.isSelected()) return SessionType.LECTURE;
        if (labRadio.isSelected()) return SessionType.LAB;
        if (tutorialRadio.isSelected()) return SessionType.TUTORIAL;
        return null;
    }

    private void loadModules() {
        List<Module> modules = moduleRepo.findAll();
        if (modules.isEmpty()) {
            moduleRepo.save(new Module("ALGO_L3", "Algorithmics L3", 3));
            moduleRepo.save(new Module("RESEAU_L3", "Networks L3", 3));
            moduleRepo.save(new Module("CRYPTO_L3", "Cryptography L3", 3));
            modules = moduleRepo.findAll();
        }
        moduleCombo.getItems().setAll(modules);
        moduleCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Module m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : m.getModuleCode() + " — " + m.getModuleName());
            }
        });
        moduleCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Module m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : m.getModuleCode() + " — " + m.getModuleName());
            }
        });

    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                if (ni.isLoopback()) continue;
                if (!ni.isUp())      continue;

                String name = ni.getDisplayName().toLowerCase();
                if (name.contains("virtual"))  continue;
                if (name.contains("vethernet")) continue;
                if (name.contains("hyper-v"))  continue;
                if (name.contains("wsl"))      continue;
                if (name.contains("loopback")) continue;
                if (name.contains("cowork"))   continue;
                if (name.contains("vmware"))   continue;
                if (name.contains("virtualbox")) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!(addr instanceof Inet4Address)) continue;
                    if (addr.isLoopbackAddress())        continue;

                    String ip = addr.getHostAddress();
                    if (ip.startsWith("192.168.") ||
                            ip.startsWith("10.")       ||
                            ip.startsWith("172.")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("getLocalIp() failed: " + e.getMessage());
        }
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "127.0.0.1"; }
    }

    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }
}