package com.qroll.server;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private static  final int MAX_REQUESTS = 10 ;
    private static final long WINDOW_MS = 60_000L;

    private record Entry(long windowStart , int count )
    {}

    private final Map<String , Entry> table = new ConcurrentHashMap<>();

    public synchronized boolean isAllowed(String ip )
    {
        long now = Instant.now().toEpochMilli();
        Entry entry = table.get(ip);

        if(entry == null || (now - entry.windowStart()) > WINDOW_MS)
        {
            table.put(ip , new Entry(now , 1));
            return true ;
        }
        if (entry.count() >= MAX_REQUESTS )
        {
            return false ;
        }

        table.put(ip , new Entry(entry.windowStart() , entry.count() + 1 ));
        return true ;
    }


    public synchronized void pruneStaleEntries()
    {
        long now = Instant.now().toEpochMilli();
        table.entrySet().removeIf(e -> now - e.getValue().windowStart() > WINDOW_MS*2 );
    }



}
