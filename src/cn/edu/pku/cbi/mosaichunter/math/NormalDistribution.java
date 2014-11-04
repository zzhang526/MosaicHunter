package cn.edu.pku.cbi.mosaichunter.math;

public class NormalDistribution {
    
    private static double EPS = 1e-9;
    private static double K = 1.0 / Math.sqrt(Math.PI * 2);
    private static double P1 = 0.15865525495718552;
    
    private NormalDistribution() {
        // private constructor 
    }
    
    public static double pNormal(double x) {
        if (x < -100 || x > 100) {
            return 0;
        }
        
        boolean positive = false;
        if (x > 0) {
            x = -x;
            positive = true;
        }        
        
        double p;
        if (x < -1) {
          p = calc(-100, x);
        } else {         
          p = P1 + calc(-1, x);
        }
        if (positive) {
            p = 1 - p;
        }
        return p;
    }
    
    private static double calc(double a, double b) {
        return calc(a, f(a), b, f(b));
    }
    
    private static double calc(double a, double va, double b, double vb) {
        double c = (a + b) / 2;
        double vc = f(c);
        if (Math.abs(vc + vc - va - vb) < EPS) {
            return (va + vb) * (b - a) / 2;
        }
        return calc(a, va, c, vc) + calc(c, vc, b, vb);
    }
    
    private static double f(double x) {
        return K * Math.exp(-x * x * 0.5);
    }
}
