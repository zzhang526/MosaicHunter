package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.FishersExactTest;

public class StrandBiasFilter extends BaseFilter {

    public static final double DEFAULT_MIN_P_VALUE = 0.05;
    
    private final double minPValue;
    
    public StrandBiasFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDouble(name, "min_p_value", DEFAULT_MIN_P_VALUE));
    }
    
    public StrandBiasFilter(String name, double minPValue) {
        super(name);
        this.minPValue = minPValue;        
    }
    
    @Override
    public boolean doFilter(Site filterEntry) {
        double p = FishersExactTest.twoSided(
                filterEntry.getPositiveMajorAlleleCount(),
                filterEntry.getPositiveMinorAlleleCount(),
                filterEntry.getNegativeMajorAlleleCount(),
                filterEntry.getNegativeMinorAlleleCount());
        filterEntry.setMetadata(
                getName(),
                new Object[] {
                    filterEntry.getPositiveMajorAlleleCount(), 
                    filterEntry.getPositiveMinorAlleleCount(), 
                    filterEntry.getNegativeMajorAlleleCount(), 
                    filterEntry.getNegativeMinorAlleleCount(),
                    p});
        
        return p >= minPValue;
    }    
}
