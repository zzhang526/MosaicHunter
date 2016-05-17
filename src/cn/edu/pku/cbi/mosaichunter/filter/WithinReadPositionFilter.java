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

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.WilcoxonRankSumTest;

public class WithinReadPositionFilter extends BaseFilter {

    public static final double DEFAULT_P_VALUE_CUTOFF = 0.05;
    
    private final double pValueCutoff;
    
    public WithinReadPositionFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDouble(name, "p_value_cutoff", DEFAULT_P_VALUE_CUTOFF));
    }
    
    public WithinReadPositionFilter(String name, double pValueCutoff) {
        super(name);
        this.pValueCutoff = pValueCutoff;        
    }
    
    @Override
    public boolean doFilter(Site site) {
        double[] majorAllelePositions = new double[site.getMajorAlleleCount()];
        double[] minorAllelePositions = new double[site.getMinorAlleleCount()];
        int i1 = 0;
        int i2 = 0;
        StringBuilder majorPos = new StringBuilder();
        StringBuilder minorPos = new StringBuilder();
        for (int i = 0; i < site.getDepth(); ++i) {
            if (site.getBases()[i] == site.getMajorAllele()) {
                majorAllelePositions[i1] = site.getBasePos()[i];
                majorPos.append(i1 > 0 ? "," : "").append(site.getBasePos()[i] + 1);
                i1++;
            }
            if (site.getBases()[i] == site.getMinorAllele()) {
                minorAllelePositions[i2] = site.getBasePos()[i];
                minorPos.append(i2 > 0 ? "," : "").append(site.getBasePos()[i] + 1);
                i2++;
            }
        }
        
        double p = WilcoxonRankSumTest.twoSided(majorAllelePositions, minorAllelePositions);
        site.setMetadata(
                getName(),
                new Object[] {
                    majorPos,
                    minorPos,
                    p});
        return p >= pValueCutoff;
    }
    
}
