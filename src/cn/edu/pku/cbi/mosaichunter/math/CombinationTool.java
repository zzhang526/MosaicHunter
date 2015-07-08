package cn.edu.pku.cbi.mosaichunter.math;

public class CombinationTool {
    
    public static final int DEFAULT_MAX = 10000000;
    public static final int DEFAULT_MAX_CACHED = 2000;
    
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
   
}
