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