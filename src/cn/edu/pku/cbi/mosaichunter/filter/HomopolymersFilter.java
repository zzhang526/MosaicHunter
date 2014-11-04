package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.IOException;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.ReferenceReader;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class HomopolymersFilter extends BaseFilter {

    public static final int DEFAULT_SHORT_HOMOPOLYMER_LENGTH = 4;
    public static final int DEFAULT_LONG_HOMOPOLYMER_LENGTH = 6;
    public static final int DEFAULT_SHORT_HOMOPOLYMER_EXPANSION = 2;
    public static final int DEFAULT_LONG_HOMOPOLYMER_EXPANSION = 3;
    
    private final int shortHomopolymerLength;
    private final int longHomopolymerLength;
    private final int shortHomopolymerExpansion;
    private final int longHomopolymerExpansion;
    private final String referenceFile;
    private ReferenceReader reader;
    
    private boolean done = false;
    private int currentChrId = 0;
    private String currentChr = "";
    private int currentRangeStart = 0;
    private int currentRangeEnd = 0;
    private byte currentRangeBase = 0;
    private int currentRangeLength = 0;
    private ReferenceReader.Entry lastEntry = null;
    private int lastBaseCount = 0;
    
    public HomopolymersFilter(String name) {
        this(name,
             ConfigManager.getInstance().get(null, "reference_file"),
             ConfigManager.getInstance().getInt(
                     name, "short_homopolymer_length", DEFAULT_SHORT_HOMOPOLYMER_LENGTH),
             ConfigManager.getInstance().getInt(
                     name, "long_homopolymer_length", DEFAULT_LONG_HOMOPOLYMER_LENGTH),
             ConfigManager.getInstance().getInt(
                     name, "short_homopolymer_expansion", DEFAULT_SHORT_HOMOPOLYMER_EXPANSION),
             ConfigManager.getInstance().getInt(
                     name, "long_homopolymer_expansion", DEFAULT_LONG_HOMOPOLYMER_EXPANSION));
    }
    
    public HomopolymersFilter(String name, String referenceFile,
            int shortHomopolymerLength, int longHomopolymerLength, 
            int shortHomopolymerExpansion, int longHomopolymerExpansion) {
        super(name);
        this.referenceFile = referenceFile;
        this.shortHomopolymerLength = shortHomopolymerLength;
        this.longHomopolymerLength = longHomopolymerLength;
        this.shortHomopolymerExpansion = shortHomopolymerExpansion;
        this.longHomopolymerExpansion = longHomopolymerExpansion;
    }
    
    @Override
    public void init() throws Exception {
        super.init();
        reader = new ReferenceReader(referenceFile);                   
    }
    
    @Override
    public void close() throws Exception {
        reader.close();
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {      
        if (done) {
            return false;
        }
        int chrId = MosaicHunterHelper.getChrId(filterEntry.getChrName());
        boolean validRange = true;
        while (!done && chrId > currentChrId) {
            validRange = nextRange();
        }                
        while (!done && chrId == currentChrId && filterEntry.getRefPos() > currentRangeEnd) {
            validRange = nextRange();
        }
        if (!done && validRange && chrId == currentChrId &&
            filterEntry.getRefPos() >= currentRangeStart &&
            filterEntry.getRefPos() <= currentRangeEnd) {
            filterEntry.setMetadata(
                    getName(),
                    new Object[] {
                        currentChr, 
                        currentRangeStart, 
                        currentRangeEnd, 
                        (char) currentRangeBase,
                        currentRangeLength});
            return false;
        }
        return true;
    }
    
    private boolean nextRange() {
        for (;;) {
            ReferenceReader.Entry entry = null;
            try {
                 entry = reader.next();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (entry != null && 
                (entry.getChrId() == 0 || entry.getBase() == 'N' || entry.getBase() == 'n')) {
                continue;
            }
            if (entry == null) {
                done = true;
            }
            if (entry == null || lastEntry == null ||
                entry.getChrId() != lastEntry.getChrId() || entry.getBase() != lastEntry.getBase()) {
                if (lastBaseCount >= shortHomopolymerLength) {
                    int expansion = lastBaseCount < longHomopolymerLength ?
                            shortHomopolymerExpansion : longHomopolymerExpansion;
                    currentChr = lastEntry.getChr();
                    currentRangeStart = lastEntry.getPosition() - expansion;
                    currentRangeEnd = lastEntry.getPosition() + lastBaseCount - 1 + expansion;
                    currentChrId = lastEntry.getChrId();
                    currentRangeBase = lastEntry.getBase();
                    currentRangeLength = lastBaseCount;
                    lastBaseCount = 1;
                    lastEntry = entry; 
                    return true;
                } 
                lastBaseCount = 1;
                lastEntry = entry; 
                if (entry == null) {
                    return false;
                }                
            } else {
                lastBaseCount++;
            }
        }
    }    
}
