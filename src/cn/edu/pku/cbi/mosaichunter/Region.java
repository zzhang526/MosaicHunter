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

package cn.edu.pku.cbi.mosaichunter;

public class Region implements Comparable<Region> {

    private final String chr;
    private final int chrId;
    private final int start;
    private final int end;
    
    public Region(String chr, int chrId, int start, int end) {
        this.chr = chr;
        this.chrId = chrId;
        this.start = start;
        this.end = end;
    }
    
    public String getChr() {
        return chr;
    }

    public int getChrId() {
        return chrId;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
    
    public int compareTo(Region that) {
        if (this.chrId > that.chrId) {
            return 1;
        } else if (this.chrId < that.chrId) {
            return -1;
        } else if (this.start > that.start) {
            return 1;
        } else if (this.start < that.start) {
            return -1;
        } else if (this.end > that.end) {
            return 1;
        } else if (this.end < that.end) {
            return -1;
        } else {
            return 0;
        }
    }
  
}