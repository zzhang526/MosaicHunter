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

import net.sf.samtools.SAMRecord;

public class ReadsCache {
    
    public final int DEFAULT_BUCKSIZE = 51;
    
    private final int bucketNumber;
    private final int bucketSize;
    private final SAMRecord[][] buckets;
    private final int[] bucketIds;

    public ReadsCache(int bucketNumber) {
        this.bucketNumber = bucketNumber;
        bucketSize = DEFAULT_BUCKSIZE;
        buckets = new SAMRecord[bucketNumber][bucketSize];
        bucketIds = new int[bucketNumber];
    }
    
    public void cacheRead(SAMRecord read) {
        int n = read.getAlignmentStart() % bucketNumber;
        buckets[n][bucketIds[n]] = read;
        bucketIds[n]++;
        if (bucketIds[n] >= bucketSize) {
            bucketIds[n] = 0;
        }
    }
    
    public SAMRecord getMate(SAMRecord read) {
        int n = read.getMateAlignmentStart();
        if (n <= 0) {
            return null;
        }
        n %= bucketNumber;
        int i = bucketIds[n];
        int k = 0;
        SAMRecord mate = null;
        for (;;) {
            k++;
            i--;
            if (i < 0) {
                i = bucketSize - 1;
            }
            
            if (buckets[n][i] != null &&
                buckets[n][i].getReadName().equals(read.getReadName()) &&
                buckets[n][i].getAlignmentStart() != read.getAlignmentStart()) {
                mate = buckets[n][i];
                break;
            }
            if (i == bucketIds[n]) {
                break;
            }
        }
        StatsManager.count("get_mate_iterations", k);
        return mate;
    }
}