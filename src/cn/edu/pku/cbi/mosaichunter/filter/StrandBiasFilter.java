package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.FishersExactTest;

public class StrandBiasFilter extends BaseFilter {

    public static final double DEFAULT_P_VALUE_CUTOFF = 0.05;
    
    private final double pValueCutoff;
    
    public StrandBiasFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDouble(name, "p_value_cutoff", DEFAULT_P_VALUE_CUTOFF));
    }
    
    public StrandBiasFilter(String name, double pValueCutoff) {
        super(name);
        this.pValueCutoff = pValueCutoff;        
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
        
        return p >= pValueCutoff;
    }    
}
