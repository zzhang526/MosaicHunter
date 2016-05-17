/*
 * The MIT License
 *
 * Copyright (c) 2016 Center for Bioinformatics, Peking University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
