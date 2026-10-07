package com.qroll.server;
import com.qroll.model.Session;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "com.qroll.server")
public class AttendanceServer {
    private ConfigurableApplicationContext ctx ;
    private volatile boolean running = false ;


    public void start(Session session )
    {
        if(running)
        {
            System.out.println("AttendanceServer already running ");
            return ;
        }

        if (isPortInUse(8080)) {
            System.out.println("AttendanceServer: port 8080 still in use, waiting...");
            for (int i = 0; i < 10; i++) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                if (!isPortInUse(8080)) break;
            }
            if (isPortInUse(8080)) {
                System.err.println("AttendanceServer: port 8080 still blocked after 5s. " +
                        "Kill the process manually: netstat -ano | findstr :8080");
                return;
            }
        }


        Thread serevrThread = new Thread(() ->
        {
            System.out.println("AttendanceServer Starting on Port 8080");
            ctx =  SpringApplication.run(AttendanceServer.class);
            ScanController controller= ctx.getBean(ScanController.class);
            controller.setActiveSession(session);
            running = true ;
            System.out.println("AttendaceSerevr: ready - http://localhost:8080");
        });

        serevrThread.setDaemon(true);
        serevrThread.setName("spring-server-thread");
        serevrThread.start();
    }

    public void stop() {
        if (ctx == null) return;

        try {
            ctx.getBean(ScanController.class).clearActiveSession();
        } catch (Exception ignored) {}

        try {
            ctx.close();
            System.out.println("AttendanceServer: Spring context closed.");
        } catch (Exception e) {
            System.err.println("AttendanceServer: error closing context — " + e.getMessage());
        }

        ctx     = null;
        running = false;

        for (int i = 0; i < 20; i++) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            if (!isPortInUse(8080)) {
                System.out.println("AttendanceServer: port 8080 released.");
                return;
            }
        }
        System.out.println("AttendanceServer: port 8080 may still be held. " +
                "Wait a moment before starting a new session.");
    }

    public boolean isRunning()
    {
        return running ;
    }

    public ScanController getScanController()
    {
        if(ctx == null ) return null ;
        return ctx.getBean(ScanController.class);
    }

    private boolean isPortInUse(int port) {
        try (java.net.ServerSocket ss = new java.net.ServerSocket(port)) {
            ss.setReuseAddress(true);
            return false;
        } catch (java.io.IOException e) {
            return true;
        }
    }
}

