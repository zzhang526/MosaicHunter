package cn.edu.pku.cbi.mosaichunter.filter;

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
        this.shortHomopolymerLength = shortHomopolymerLength;
        this.longHomopolymerLength = longHomopolymerLength;
        this.shortHomopolymerExpansion = shortHomopolymerExpansion;
        this.longHomopolymerExpansion = longHomopolymerExpansion;
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {   
        
        long pos = filterEntry.getRefPos();
        int width = Math.max(
                longHomopolymerLength + longHomopolymerExpansion, 
                shortHomopolymerLength + shortHomopolymerExpansion);
        
        long left = filterEntry.getRefPos() - width + 1;
        long right = filterEntry.getRefPos() + width - 1;
        byte lastBase = 'N';
        int cnt = 0;
        for (long i = left; i <= right + 1; ++i) {
            byte base = filterEntry.getReferenceManager().getBase(
                    filterEntry.getChrName(), i);
            if (base == 'N' || base != lastBase || i == right + 1) {
                if ((cnt >= longHomopolymerLength && 
                     (i - cnt - longHomopolymerExpansion <= pos && i - 1 + longHomopolymerExpansion >= pos)) || 
                    (cnt >= shortHomopolymerLength && 
                     (i - cnt - shortHomopolymerExpansion <= pos && i - 1 + shortHomopolymerExpansion >= pos))) {
                    return false;
                } 
                cnt = 0;
            }
            if (base != 'N') {
                cnt++;
            }
            lastBase = base;
        }
        
        return true;
    }
}
