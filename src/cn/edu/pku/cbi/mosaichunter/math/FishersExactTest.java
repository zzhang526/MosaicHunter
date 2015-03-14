package cn.edu.pku.cbi.mosaichunter.math;

public class FishersExactTest {

    private FishersExactTest() {
        // private constructor 
    }
    
    public static double twoSided(int a, int b, int c, int d) {
        if (a > c) {
            int tmp = a;
            a = c;
            c = tmp;
            tmp = b;
            b = d;
            d = tmp;
        }
        int ac = a + c;
        int bd = b + d;        
        int ab = a + b;
        int cd = c + d;
        
        double all = CombinationTool.cLog(a + b + c + d, ac);     
        
        double base = CombinationTool.cLog(ab, a) + CombinationTool.cLog(cd, c) - all;
        base *= 1 - 1e-8;

        
        double p = 0;
        
        
        for (int aa = 0; aa <= ac; aa++) {
            int bb = ab - aa;
            int cc = ac - aa;
            int dd = cd - cc;            
            if (bb < 0 || bb > bd || bb > ab) {
                continue;
            }  
            if (cc < 0 || cc > ac || cc > cd) {
                continue;
            }  
            if (dd < 0 || dd > cd || dd > bd) {
                continue;
            }
            
            double q = CombinationTool.cLog(ab, aa) + CombinationTool.cLog(cd, cc) - all;
            if (q <= base) {
                p += Math.exp(q);
            }            
        }    
        return p;
    }
   
}
