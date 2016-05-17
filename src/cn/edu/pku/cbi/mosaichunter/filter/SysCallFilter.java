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

import net.sf.samtools.SAMRecord;
import cn.edu.pku.cbi.mosaichunter.BamSiteReader;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.reference.ReferenceManager;

public class SysCallFilter extends BaseFilter {
   
    private final double[] base0Weight;
    private final double[] base1Weight;
    private final double[] base2Weight;
    private final double intercept;
    private final double afDiffWeight;
    private final double altAfWeight;
    private final double tTestWeight;
      
    private BamSiteReader siteReader = null;
    
    public SysCallFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDoubles(getNamesapce(name) , "base0"),
             ConfigManager.getInstance().getDoubles(getNamesapce(name) , "base1"),
             ConfigManager.getInstance().getDoubles(getNamesapce(name) , "base2"),
             ConfigManager.getInstance().getDouble(getNamesapce(name) , "intercept"),
             ConfigManager.getInstance().getDouble(getNamesapce(name) , "diff_err_diff_dir"),
             ConfigManager.getInstance().getDouble(getNamesapce(name) , "diff_error_dir"),
             ConfigManager.getInstance().getDouble(getNamesapce(name) , "t_test"));
    }
    
    public SysCallFilter(String name, 
            double[] base0, double[] base1, double[] base2,
            double intercept, double afDiff, double altAf, double tTest) {
        super(name);
        this.base0Weight = base0;
        this.base1Weight = base1;
        this.base2Weight = base2;
        this.intercept = intercept;
        this.afDiffWeight = afDiff;
        this.altAfWeight = altAf;
        this.tTestWeight = tTest;
    }        
    
    private static String getNamesapce(String name) {
        int depth = ConfigManager.getInstance().getInt(name, "depth", -1);
        int[] depths = ConfigManager.getInstance().getInts(name, "training_depths");
        if (depth < 0 || depths.length == 0) {
            return name;
        }
        int d = depths[0];
        for (int i = 1; i < depths.length; ++i) {
            if (Math.abs(depth - d) > Math.abs(depth - depths[i])) {
                d = depths[i];
            }
        }
        return name + "." + d;
    }
    
    @Override
    public void init(MosaicHunterContext context) throws Exception {
        super.init(context);
        
        String inputFile = ConfigManager.getInstance().get(null, "input_file", null);
        String indexFile = ConfigManager.getInstance().get(null, "index_file", null);
        int maxDepth = ConfigManager.getInstance().getInt(null, "max_depth");
        boolean removeDuplicates = ConfigManager.getInstance().getBoolean(
                null, "remove_duplicates", true);
        int removeFlags = ConfigManager.getInstance().getIntFlags(null, "remove_flags", 0);
        
        siteReader = new BamSiteReader(
                getContext().getReferenceManager(), inputFile, indexFile, 
                maxDepth, 0, 0, removeDuplicates, removeFlags);
        siteReader.init();
    }
    
    @Override
    public void close() throws Exception {
        siteReader.close();
    }
    
    @Override
    public boolean doFilter(Site site) {
        Site rawSite;
        try {
            rawSite = siteReader.read(
                    site.getRefName(), site.getRefPos(), site.getRef(), null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        int refPos = (int) rawSite.getRefPos();
        if (refPos < 3) {
            return false;
        }

        int refPosCount;
        int altPosCount;
        int refNegCount;
        int altNegCount;
        
        if (rawSite.getMajorAllele() == rawSite.getRef()) {
            refPosCount = rawSite.getPositiveMajorAlleleCount();
            refNegCount = rawSite.getNegativeMajorAlleleCount();
        } else if (rawSite.getMinorAllele() == rawSite.getRef()) {
            refPosCount = rawSite.getPositiveMinorAlleleCount();
            refNegCount = rawSite.getNegativeMinorAlleleCount();
        } else {
            return false;
        }
        altPosCount = rawSite.getPositiveAlleleCount() - refPosCount;
        altNegCount = rawSite.getNegativeAlleleCount() - refNegCount;
        
        String refName = rawSite.getRefName();
        double altPosAf = (double) altPosCount / (altPosCount + refPosCount);
        double altNegAf = (double) altNegCount / (altNegCount + refNegCount);
        double altAf;
        byte base0;
        byte base1;
        byte base2;
        
        ReferenceManager referenceManager = getContext().getReferenceManager();
        if (altPosAf > altNegAf) {
            base2 = referenceManager.getBase(refName, refPos - 2);
            base1 = referenceManager.getBase(refName, refPos - 1);
            base0 = rawSite.getRef();
            altAf = altPosAf;
        } else {
            base2 = getComplementaryBase(referenceManager.getBase(refName, refPos + 2));
            base1 = getComplementaryBase(referenceManager.getBase(refName, refPos + 1));
            base0 = getComplementaryBase(rawSite.getRef());
            altAf = altNegAf;
        }
        
        if (base2 == 'N' || base1 == 'N' || base0 == 'N') {
            return false;
        }
        if (altPosCount + refPosCount == 0 || altNegCount + refNegCount == 0) {
            return false;
        }
        
        double afDiff = Math.abs(altPosAf - altNegAf);
        
        boolean tTestNegStrand = altNegAf > altPosAf;
        double diffSum = 0;
        double diff2Sum = 0;
        
        int n = 0;
        for (int i = 0; i < rawSite.getDepth(); ++i) {
            SAMRecord r = rawSite.getReads()[i];
            int pos = rawSite.getBasePos()[i];
            int nextPos = tTestNegStrand ? pos - 1 : pos + 1;
            if (r.getReadNegativeStrandFlag() == tTestNegStrand &&
                nextPos < r.getReadLength() && nextPos >= 0) {
                double diff = r.getBaseQualities()[pos] - r.getBaseQualities()[nextPos];
                diffSum += diff;
                diff2Sum += diff * diff;
                n++;
            }
        }
        if  (n < 2) {
            return false;
        }
        double tTest = diffSum / Math.sqrt((n * diff2Sum - diffSum * diffSum) / (n - 1));
        
        double[] feature = new double[7];
        feature[0] = base2Weight[MosaicHunterHelper.BASE_TO_ID[base2]];
        feature[1] = base1Weight[MosaicHunterHelper.BASE_TO_ID[base1]];
        feature[2] = base0Weight[MosaicHunterHelper.BASE_TO_ID[base0]];
        feature[3] = afDiff * afDiffWeight;
        feature[4] = altAf * altAfWeight;
        feature[5] = tTest * tTestWeight;
        feature[6] = intercept;
        
        double featureSum = 0;
        for (int i = 0; i < feature.length; ++i) {
            featureSum += feature[i];
        }
        
        double p = 1 / (1 + Math.exp(-featureSum));
        
        site.setMetadata(
                getName(),
                new Object[] {
                    refPosCount,
                    altPosCount,
                    altPosAf,
                    refNegCount,
                    altNegCount,
                    altNegAf,
                    (char) base2,
                    (char) base1,
                    (char) base0,
                    afDiff,
                    altAf,
                    tTest,
                    p});
        
        return p < 0.5;
        
    }    
    
    private byte getComplementaryBase(byte base) {
        if (base == 'A') {
            return 'T';
        } else if (base == 'T') {
            return 'A';
        } else if (base == 'C') {
            return 'G';
        } else if (base == 'G') {
            return 'C';
        } else {
            return 'N';
        } 
    }
}
