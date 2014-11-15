package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.Arrays;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.WilcoxonRankSumTest;

public class ReadQualityFilter extends BaseFilter {

    public static final double DEFAULT_MIN_P_VALUE = 0.05;
    
    private final double minPValue;
    
    public ReadQualityFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDouble(name, "min_p_value", DEFAULT_MIN_P_VALUE));
    }
    
    public ReadQualityFilter(String name, double minPValue) {
        super(name);
        this.minPValue = minPValue;        
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {
        double[] majorAlleleQualities = new double[filterEntry.getMajorAlleleCount()];
        double[] minorAlleleQualities = new double[filterEntry.getMinorAlleleCount()];
        int i1 = 0;
        int i2 = 0;
        for (int i = 0; i < filterEntry.getDepth(); ++i) {
            if (filterEntry.getBases()[i] == filterEntry.getMajorAllele()) {
                majorAlleleQualities[i1] = filterEntry.getBaseQualities()[i];
                i1++;
            }
            if (filterEntry.getBases()[i] == filterEntry.getMinorAllele()) {
                minorAlleleQualities[i2] = filterEntry.getBaseQualities()[i];
                i2++;
            }
        }
        Arrays.sort(majorAlleleQualities);
        Arrays.sort(minorAlleleQualities);

        StringBuilder majorQualities = new StringBuilder();
        StringBuilder minorQualities = new StringBuilder();
        
        for (int i = 0; i < majorAlleleQualities.length; ++i) {
            majorQualities.append(i > 0 ? "," : "").append((int) majorAlleleQualities[i]);
        }
        for (int i = 0; i < minorAlleleQualities.length; ++i) {
            minorQualities.append(i > 0 ? "," : "").append((int) minorAlleleQualities[i]);
        }
        
        double p = WilcoxonRankSumTest.twoSided(majorAlleleQualities, minorAlleleQualities);
        filterEntry.setMetadata(
                getName(),
                new Object[] {
                    majorQualities,
                    minorQualities,
                    p});
        return p >= minPValue;
    }
    
}
