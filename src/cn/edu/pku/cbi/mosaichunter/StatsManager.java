package cn.edu.pku.cbi.mosaichunter;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class StatsManager {

    public static final Map<String, Walltime> walltime = new HashMap<String, Walltime>();
    public static final Map<String, Counter> counters = new HashMap<String, Counter>();

    public static final DecimalFormat format = new DecimalFormat("0.000");

    private static final boolean enableTimer;
    private static final boolean enableCounter;
    
    static {
        enableTimer = ConfigManager.getInstance().getBoolean(
                "stats_manager", "enable_timer", false);
        enableCounter = ConfigManager.getInstance().getBoolean(
                "stats_manager", "enable_counter", false);
    }
    
    private StatsManager() {
    }
    
    public static void count(String name) {
        count(name, 1);
    }
    
    public static void count(String name, long n) {
        if (!enableCounter) {
            return;
        }
        Counter c = getCounter(name);
        c.total += n;
        c.cnt++;
    }
    public static void start(String name) {
        if (!enableTimer) {
            return;
        }
        Walltime wt = getWalltime(name);
        if (wt.last > 0) {
            wt.error++;
            return;   
        }
        wt.last = System.nanoTime();
    }
    
    public static void end(String name) {
        if (!enableTimer) {
            return;
        }
        Walltime wt = getWalltime(name);
        if (wt.last <= 0) {
            wt.error++;
            return;
            
        }
        wt.total += System.nanoTime() - wt.last;
        wt.cnt++;
        wt.last = 0;
    }
    
    public static void printStats() {
        if (enableTimer) {
            System.out.println("Timers:");
            for (String name : walltime.keySet()) {
                Walltime wt = getWalltime(name);
                double total = wt.total / 1000000.0;
                double avg = total / wt.cnt;
                String lineFormat = "%1$-30s %2$15s %3$15s %4$15s";
                System.out.println(String.format(lineFormat,
                        name + (wt.error > 0 ? "(!)" : ""),
                        format.format(total),
                        String.valueOf(wt.cnt),
                        format.format(avg)));
            }
        }
        
        if (enableCounter) {
            System.out.println("Counters:");
            for (String name : counters.keySet()) {
                Counter c = getCounter(name);
                
                String lineFormat = "%1$-30s %2$15s %3$15s %4$15s";
                if (c.total != c.cnt) {
                    System.out.println(String.format(lineFormat,
                            name,
                            c.total,
                            c.cnt,
                            c.total / c.cnt));
                } else {
                    System.out.println(String.format(lineFormat,
                            name,
                            c.total,
                            "",
                            ""));
             
                }
            }
        }
    }
    
    private static Walltime getWalltime(String name) {
        Walltime wt = walltime.get(name);
        if (wt == null) {
            wt = new Walltime();
            walltime.put(name,  wt);
        } 
        return wt;
    }
    
    private static Counter getCounter(String name) {
        Counter c = counters.get(name);
        if (c == null) {
            c = new Counter();
            counters.put(name, c);
        } 
        return c;
    }
    
    private static class Walltime {
        private long last;
        private long total;
        private long cnt;
        private long error;
    }
    
    private static class Counter {
        private long total;
        private long cnt;
    }
}
