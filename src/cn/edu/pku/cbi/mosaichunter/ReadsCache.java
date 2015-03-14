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