package cn.edu.pku.cbi.mosaichunter.math;

public class CombinationTool {
    
    public static final int DEFAULT_MAX = 10000000;
    public static final int DEFAULT_MAX_CACHED = 2000;
    
    private static double[][] c = new double[DEFAULT_MAX_CACHED + 1][DEFAULT_MAX_CACHED + 1];
    private static double[] logFactorial = new double[DEFAULT_MAX + 1];

    static {
        logFactorial[0] = 0.0;
        for (int i = 1; i < logFactorial.length; i++) {
            logFactorial[i] = logFactorial[i-1] + Math.log(i);
        }
    }

    private CombinationTool() {
        // private constructor 
    }
    
    // select n from m (n <= m)
    public static double c(int m, int n) {
        if (n > m) {
            throw new IllegalArgumentException("n(" + n + ") exceeds m( " +  m + ")");            
        }
        if (n < 0) {
            throw new IllegalArgumentException("n(" + n + ") is less than zero");            
        }
        if (m < c.length) {
            return calc(m, n);
        } else if (m < logFactorial.length) {
            return Math.exp(logFactorial[m] - logFactorial[m - n] - logFactorial[n]);
        } else {
            throw new IllegalArgumentException(
                    "m(" + m + ") exceeds limit " +  logFactorial.length);            
        }
    }
    
    public static double cLog(int m, int n) {
        if (n > m) {
            throw new IllegalArgumentException("n(" + n + ") exceeds m( " +  m + ")");            
        }
        if (n < 0) {
            throw new IllegalArgumentException("n(" + n + ") is less than zero");            
        }
        if (m < logFactorial.length) {
            return logFactorial[m] - logFactorial[m - n] - logFactorial[n];
        } else {
            throw new IllegalArgumentException(
                    "m(" + m + ") exceeds limit " +  logFactorial.length);            
        }
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
