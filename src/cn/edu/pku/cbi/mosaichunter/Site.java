package cn.edu.pku.cbi.mosaichunter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.SAMRecord;

public class Site {

    public static final String DEFAULT_ALLELE_ID_ORDER = "ACGT";
    
    private static final Map<String, int[]> alleleIdCache = new HashMap<String, int[]>();
    private static final int[] defaultAlleleId;
    static {
        defaultAlleleId = getAlleleId(DEFAULT_ALLELE_ID_ORDER);
    }
    
    private final int maxDepth; 

    private final SAMRecord[] reads;
    private final byte[] bases;
    private final byte[] baseQualities;
    private final short[] basePos;
    private final Map<String, Object[]> metadata = new HashMap<String, Object[]>(); 
    private final Set<String> passedFilters = new HashSet<String>();
    private final int[] alleleCount = new int[4];
    
    private String refName;
    private int refId;
    private long refPos;
    private byte ref;
    private int depth;
    private int realDepth;
    private String alleleIdOrder;
    private int[] alleleId;
    
    private byte majorAllele = -1;
    private byte minorAllele = -1;
    private int majorAlleleId = -1;
    private int minorAlleleId = -1;
    private int majorAlleleCount = -1;
    private int minorAlleleCount = -1;
    private int positiveAlleleCount = -1;    
    private int positiveMajorAlleleCount = -1;    
    private int positiveMinorAlleleCount = -1;
    private int negativeAlleleCount = -1;
    private int negativeMajorAlleleCount = -1;    
    private int negativeMinorAlleleCount = -1;
    
    private String alleleCountOrder;

    public Site(int maxDepth) {
        this.maxDepth = maxDepth;
        bases = new byte[maxDepth];
        baseQualities = new byte[maxDepth];
        basePos = new short[maxDepth];
        reads = new SAMRecord[maxDepth];
    }
    
    // TODO base/baseQ
    public Site(String refName, int refId, long refPos, byte ref, int depth, int realDepth,
            byte[] bases, byte[] baseQualities, SAMRecord[] reads, short[] basePos) {
        this(refName, refId, refPos, ref, depth, realDepth, bases, baseQualities, reads, basePos, null);
    }
    
    public Site(String refName, int refId, long refPos, byte ref, int depth, int realDepth,
            byte[] bases, byte[] baseQualities, SAMRecord[] reads, short[] basePos, 
            String alleleIdOrder) {
        this.maxDepth = depth;
        this.bases = bases;
        this.baseQualities = baseQualities;
        this.reads = reads;
        this.basePos = basePos;
        init(refName, refId, refPos, ref, depth, realDepth, alleleIdOrder);
    }
    
    private static int[] getAlleleId(String alleleIdOrder) {
        int[] alleleId = alleleIdCache.get(alleleIdOrder); 
        if (alleleId == null) {
            alleleId = new int[128];
            for (int i = 0; i < alleleIdOrder.length(); ++i) {
                alleleId[alleleIdOrder.charAt(i)] = i;
            }
            alleleIdCache.put(alleleIdOrder, alleleId);
        }
        return alleleId;
    }
    
    public void init(String refName, int refId, long refPos, byte ref, int depth, int realDepth, String alleleIdOrder) {
        this.refName = refName;
        this.refId = refId;
        this.refPos = refPos;
        this.ref = ref;
        this.depth = depth;
        this.realDepth = realDepth;
        
        if (alleleIdOrder == null) {
            this.alleleIdOrder = DEFAULT_ALLELE_ID_ORDER;
            this.alleleId = defaultAlleleId;
        } else {
            this.alleleIdOrder = alleleIdOrder;
            this.alleleId = getAlleleId(alleleIdOrder); 
        }
        metadata.clear();
        passedFilters.clear();
        alleleCount[0] = 0;
        alleleCount[1] = 0;
        alleleCount[2] = 0;
        alleleCount[3] = 0;
        
        majorAllele = -1;
    }
    
    public void copy(Site that) {
        init(that.refName, that.refId, that.refPos, that.ref, that.depth, that.realDepth, that.alleleIdOrder);
        //System.arraycopy(that.bases, 0, bases, 0, depth);
        //System.arraycopy(that.baseQualities, 0, baseQualities, 0, depth);
        System.arraycopy(that.reads, 0, reads, 0, depth);
        System.arraycopy(that.basePos, 0, basePos, 0, depth);
        metadata.putAll(that.metadata);
        passedFilters.addAll(that.passedFilters);
    }
    
    public void increaceRealDepth() {
        realDepth++;
    }
    
    public void addRead(SAMRecord read, short pos) {

        if (depth >= maxDepth) {
            return;
        }
        
        reads[depth] = read;
        basePos[depth] = pos;
        //bases[depth] = base;
        //baseQualities[depth] = baseQ;
        
        depth++;
        majorAllele = -1;
        
    }
    
    public void replaceRead(int i, SAMRecord read, short pos) {
        //bases[i] = base;
        //baseQualities[i] = baseQ;
        reads[i] = read;
        basePos[i] = pos;
        majorAllele = -1;
    }
    
    public int getMaxDepth() {
        return maxDepth;
    }
    
    public String getRefName() {
        return refName;
    }
    
    public long getRefPos() {
        return refPos;
    }
     
    public byte getRef() {
        return ref;
    }

    public int getDepth() {
        return depth;
    }

    public int getRealDepth() {
        return realDepth;
    }

    public byte[] getBases() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return bases;
    }

    public byte[] getBaseQualities() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return baseQualities;
    }
    
    public SAMRecord[] getReads() {
        return reads;
    }
    
    public short[] getBasePos() {
        return basePos;
    }
    
    public Object[] getMetadata(String name) {
        return metadata.get(name);
    }
    
    public void setMetadata(String name, Object[] data) {
        metadata.put(name, data);
    }
   
    public byte getMajorAllele() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return majorAllele;
    }
    
    public int getMajorAlleleId() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return majorAlleleId;
    }

    public byte getMinorAllele() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return minorAllele;
    }
    
    public int getMinorAlleleId() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return minorAlleleId;
    }

    public int getMajorAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return majorAlleleCount;
    }

    public int getMinorAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return minorAlleleCount;
    }

    public int getAlleleCount(byte allele) {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return alleleCount[alleleId[allele]];
    }

    public int getPositiveAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return positiveAlleleCount;
    }
    
    public int getPositiveMajorAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return positiveMajorAlleleCount;
    }

    public int getPositiveMinorAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return positiveMinorAlleleCount;
    }
    
    public int getNegativeAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return negativeAlleleCount;
    }
    
    public int getNegativeMajorAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return negativeMajorAlleleCount;
    }

    public int getNegativeMinorAlleleCount() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return negativeMinorAlleleCount;
    }
    
    public String getAlleleCountOrder() {
        if (majorAllele < 0) {
            calculateAlleleCounts();
        }
        return alleleCountOrder;
    }
    
    private void calculateAlleleCounts() {    
        int[] negativeAlleleCounts = new int[4];

        positiveAlleleCount = 0;
        negativeAlleleCount = 0;
        
        for (int i = 0; i < depth; ++i) { 
            bases[i] = reads[i].getReadBases()[basePos[i]];
            baseQualities[i] = reads[i].getBaseQualities()[basePos[i]];

            alleleCount[alleleId[bases[i]]]++;
            if (reads[i].getReadNegativeStrandFlag()) {
                negativeAlleleCounts[alleleId[bases[i]]]++;
                negativeAlleleCount++;
            } else {
                positiveAlleleCount++;
            }
        }
        int[] allele = new int[] {0, 1, 2, 3};
        for (int i = 0; i < 3; ++i) {
            for (int j = i + 1; j < 4; ++j) {
                if (alleleCount[allele[i]] < alleleCount[allele[j]] ||
                    (alleleCount[allele[i]] == alleleCount[allele[j]] && allele[i] > allele[j])) {
                    int temp = allele[i];
                    allele[i] = allele[j];
                    allele[j] = temp;
                }
                    
            }
        }
        
        majorAllele = (byte) alleleIdOrder.charAt(allele[0]);
        minorAllele = (byte) alleleIdOrder.charAt(allele[1]);
        majorAlleleId = MosaicHunterHelper.BASE_TO_ID[majorAllele];
        minorAlleleId = MosaicHunterHelper.BASE_TO_ID[minorAllele];

        majorAlleleCount = alleleCount[allele[0]];
        minorAlleleCount = alleleCount[allele[1]];;
        negativeMajorAlleleCount = negativeAlleleCounts[allele[0]];  
        negativeMinorAlleleCount = negativeAlleleCounts[allele[1]];
        positiveMajorAlleleCount = alleleCount[allele[0]] - negativeAlleleCounts[allele[0]];    
        positiveMinorAlleleCount = alleleCount[allele[1]] - negativeAlleleCounts[allele[1]];
        
        alleleCountOrder = "" + 
                alleleIdOrder.charAt(allele[0]) + 
                alleleIdOrder.charAt(allele[1]) + 
                alleleIdOrder.charAt(allele[2]) + 
                alleleIdOrder.charAt(allele[3]);                 
    }
    
    public Set<String> getPassedFilters() {
        return passedFilters;
    }

}
