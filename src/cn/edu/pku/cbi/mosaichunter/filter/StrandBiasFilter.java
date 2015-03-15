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
    public boolean doFilter(Site site) {
        double p = FishersExactTest.twoSided(
                site.getPositiveMajorAlleleCount(),
                site.getPositiveMinorAlleleCount(),
                site.getNegativeMajorAlleleCount(),
                site.getNegativeMinorAlleleCount());
        site.setMetadata(
                getName(),
                new Object[] {
                    site.getPositiveMajorAlleleCount(), 
                    site.getPositiveMinorAlleleCount(), 
                    site.getNegativeMajorAlleleCount(), 
                    site.getNegativeMinorAlleleCount(),
                    p});
        
        return p >= minPValue;
    }    
}
