/*
 * The MIT License
 *
 * Copyright (c) 2016 Center for Bioinformatics, Peking University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.Arrays;

//import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;


import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.WilcoxonRankSumTest;

public class MappingQualityFilter extends BaseFilter {

    public static final double DEFAULT_P_VALUE_CUTOFF = 0.05;
    
    private final double pValueCutoff;
    
    public MappingQualityFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDouble(name, "p_value_cutoff", DEFAULT_P_VALUE_CUTOFF));
    }
    
    public MappingQualityFilter(String name, double pValueCutoff) {
        super(name);
        this.pValueCutoff = pValueCutoff;        
    }
    
    @Override
    public boolean doFilter(Site site) {
        double[] majorAlleleQualities = new double[site.getMajorAlleleCount()];
        double[] minorAlleleQualities = new double[site.getMinorAlleleCount()];
        int i1 = 0;
        int i2 = 0;
        for (int i = 0; i < site.getDepth(); ++i) {
            if (site.getBases()[i] == site.getMajorAllele()) {
                majorAlleleQualities[i1] = site.getReads()[i].getMappingQuality();
                i1++;
            } else if (site.getBases()[i] == site.getMinorAllele()) {
                minorAlleleQualities[i2] = site.getReads()[i].getMappingQuality();
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
        site.setMetadata(
                getName(),
                new Object[] {
                    majorQualities,
                    minorQualities,
                    p});
        return p >= pValueCutoff;
    }
    
}
