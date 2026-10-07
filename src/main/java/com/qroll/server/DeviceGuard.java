package com.qroll.server;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//LIMITATION: Two students physically sharing one phone would be blocked.
public class DeviceGuard {
    private final Set<String> scannedDevices = Collections.synchronizedSet(new HashSet<>());
    public boolean isFirstScan( String ip )
    {
        return scannedDevices.add(ip);
    }
    public  void reset()
    {
        scannedDevices.clear();
        System.out.println("DeviceGuard: reset  , all devices cleared for new session");

    }
    public int scannedCount()
    {
        return scannedDevices.size();
    }
}
