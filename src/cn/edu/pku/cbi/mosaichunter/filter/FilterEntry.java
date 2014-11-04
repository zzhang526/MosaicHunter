package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.ReadCache;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

public class FilterEntry {

    public static final String DEFAULT_ALLELE_ID_ORDER = "ACGT";
    
    private final SAMFileReader samFileReader;
    private final ReadCache readCache;
    
    private final String chrName;
    private final long refPos;
    private final byte ref;
    private final int depth;
    private final byte[] bases;
    private final byte[] baseQualities;
    private final SAMRecord[] reads;
    private final SAMRecord[] mates;
    private final int[] basePos;
    private final Map<String, Object[]> metadata = new HashMap<String, Object[]>(); 
    private final Set<String> passedFilters = new HashSet<String>();
    private final String alleleIdOrder;
    private final int[] alleleId;
    private final int[] alleleCount = new int[4];
    
    
    private byte majorAllele = -1;
    private byte minorAllele = -1;
    private int majorAlleleId = -1;
    private int minorAlleleId = -1;
    private int majorAlleleCount = -1;
    private int minorAlleleCount = -1;
    private int positiveMajorAlleleCount = -1;    
    private int positiveMinorAlleleCount = -1;
    private int negativeMajorAlleleCount = -1;    
    private int negativeMinorAlleleCount = -1;
    private String alleleCountOrder;

    
    public FilterEntry(SAMFileReader samFileReader, ReadCache readCache,
            String chrName, long refPos, byte ref, int depth,
            byte[] bases, byte[] baseQualities, SAMRecord[] reads, int[] basePos) {
        this(samFileReader, readCache, chrName, refPos, ref, depth, bases, baseQualities, reads, 
             basePos, DEFAULT_ALLELE_ID_ORDER);
    }
    
    public FilterEntry(SAMFileReader samFileReader, ReadCache readCache,
            String chrName, long refPos, byte ref, int depth,
            byte[] bases, byte[] baseQualities, SAMRecord[] baseRecords, int[] basePos,
            String alleleIdOrder) {
        this.samFileReader = samFileReader;
        this.readCache = readCache;
        this.chrName = chrName;
        this.refPos = refPos;
        this.ref = ref;
        this.depth = depth;
        this.bases = bases;
        this.baseQualities = baseQualities;
        this.reads = baseRecords;
        this.mates = new SAMRecord[depth];
        this.basePos = basePos;
        this.alleleIdOrder = alleleIdOrder;
        alleleId = new int[128];
        for (int i = 0; i < alleleIdOrder.length(); ++i) {
            alleleId[alleleIdOrder.charAt(i)] = i;
        }
    }
    
    public SAMFileReader getSAMFileReader() {
        return samFileReader;
    }
    
    public ReadCache getReadCache() {
        return readCache;
    }
    
    public String getChrName() {
        return chrName;
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

    public byte[] getBases() {
        return bases;
    }

    public byte[] getBaseQualities() {
        return baseQualities;
    }
    
    public SAMRecord[] getReads() {
        return reads;
    }
    
    public SAMRecord[] getMates() {
        return mates;
    }
    
    public int[] getBasePos() {
        return basePos;
    }
    
    public Object[] getMetadata(String name) {
        return metadata.get(name);
    }
    
    public void setMetadata(String name, Object[] data) {
        metadata.put(name, data);
    }
    
    public Set<String> getPassedFilters() {
        return passedFilters;
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
        int[] negativeAlleleCount = new int[4];

        for (int i = 0; i < depth; ++i) { 
            alleleCount[alleleId[bases[i]]]++;
            if (reads[i].getReadNegativeStrandFlag()) {
                negativeAlleleCount[alleleId[bases[i]]]++;
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
        majorAlleleId = MosaicHunterHelper.getBaseId(majorAllele);
        minorAlleleId = MosaicHunterHelper.getBaseId(minorAllele);

        majorAlleleCount = alleleCount[allele[0]];
        minorAlleleCount = alleleCount[allele[1]];;
        negativeMajorAlleleCount = negativeAlleleCount[allele[0]];  
        negativeMinorAlleleCount = negativeAlleleCount[allele[1]];
        positiveMajorAlleleCount = alleleCount[allele[0]] - negativeAlleleCount[allele[0]];    
        positiveMinorAlleleCount = alleleCount[allele[1]] - negativeAlleleCount[allele[1]];
        alleleCountOrder = "" + 
                alleleIdOrder.charAt(allele[0]) + 
                alleleIdOrder.charAt(allele[1]) + 
                alleleIdOrder.charAt(allele[2]) + 
                alleleIdOrder.charAt(allele[3]);                 
    }

}
