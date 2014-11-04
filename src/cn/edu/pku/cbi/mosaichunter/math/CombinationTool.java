package cn.edu.pku.cbi.mosaichunter.math;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class CombinationTool {
    
    public static final int DEFAULT_MAX_N = 1000;
    
    private static double[][] c = null;
    
    static {
        int n = ConfigManager.getInstance().getInt(null, "max_depth", DEFAULT_MAX_N);
        if (n < DEFAULT_MAX_N) {
            n = DEFAULT_MAX_N;
        }
        c = new double[n * 2 + 1][n * 2 + 1];
    }

    private CombinationTool() {
        // private constructor 
    }
    
    // select n from m (n <= m)
    public static double c(int m, int n) {
        if (n > m) {
            throw new IllegalArgumentException("n(" + n + ") exceeds m( " +  m + ")");            
        }
        if (m > c.length) {
            throw new IllegalArgumentException("m(" + m + ") exceeds limit " +  c.length);            
        }
        if (n < 0) {
            throw new IllegalArgumentException("n(" + n + ") is less than zero");            
        }
        return calc(m, n);       
    }
    
    private static double calc(int m, int n) {
        if (c[m][n] > 0) {
            return c[m][n];
        }
        double a = 0;
        if (m == 0) {
            a = 1;
        } else {
            if (n > 0) {
                a += calc(m - 1, n - 1);
            } 
            if (n < m) {
                a += calc(m - 1, n);
            }
        }
        c[m][n] = a;
        return a;
    }
}
