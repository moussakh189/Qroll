package com.qroll.server;


import com.qroll.model.Session;
import org.springframework.boot.ConfigurableBootstrapContext;
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

    public void stop()
    {
        ctx.getBean(ScanController.class).clearActiveSession();
        ctx.close();
        ctx = null ;
        running = false ;
        System.out.println("AttendanceSerevr: Stopped ");

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

}

