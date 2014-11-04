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
        
        double all = CombinationTool.c(a + b + c + d, ac);        
        double base = CombinationTool.c(ab, a) * CombinationTool.c(cd, c) / all;
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
            if (dd < 0 || dd > cd || bb > bd) {
                continue;
            }
            
            double q = CombinationTool.c(ab, aa) * CombinationTool.c(cd, cc) / all;
            if (q < base + 1e-9) {
                p += q;
            }            
        }                
        return p;       
    }
}
