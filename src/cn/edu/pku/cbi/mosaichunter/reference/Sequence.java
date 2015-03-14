package cn.edu.pku.cbi.mosaichunter.reference;

import java.io.Serializable;

public class Sequence implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final long start;
    private final long length;
    private final long[] bases;
    
    public Sequence(long start, long length, long[] bases) {
        this.start = start;
        this.length = length;
        this.bases = bases;
    }

    public long getStart() {
        return start;
    }

    public long getLength() {
        return length;
    }

    public long[] getBases() {
        return bases;
    }
    
}