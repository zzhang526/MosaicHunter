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
