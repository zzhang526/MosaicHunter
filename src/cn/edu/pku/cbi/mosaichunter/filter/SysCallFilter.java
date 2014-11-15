package cn.edu.pku.cbi.mosaichunter.filter;

import net.sf.samtools.SAMRecord;
import cn.edu.pku.cbi.mosaichunter.BamSiteReader;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.ReferenceManager;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

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
             ConfigManager.getInstance().getDoubles(name, "base0"),
             ConfigManager.getInstance().getDoubles(name, "base1"),
             ConfigManager.getInstance().getDoubles(name, "base2"),
             ConfigManager.getInstance().getDouble(name, "intercept"),
             ConfigManager.getInstance().getDouble(name, "diff_err_diff_dir"),
             ConfigManager.getInstance().getDouble(name, "diff_error_dir"),
             ConfigManager.getInstance().getDouble(name, "t_test"));
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
    
    @Override
    public void init() throws Exception {
        super.init();
        
        String inputFile = ConfigManager.getInstance().get(null, "input_file", null);
        String indexFile = ConfigManager.getInstance().get(null, "index_file", null);
        int maxDepth = ConfigManager.getInstance().getInt(null, "max_depth");
        siteReader = new BamSiteReader(inputFile, indexFile, maxDepth, 0, 0);
        siteReader.init();
    }
    
    @Override
    public void close() throws Exception {
        siteReader.close();
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {
        FilterEntry site;
        try {
            site = siteReader.read(
                    filterEntry.getChrName(), filterEntry.getRefPos(), filterEntry.getRef(), null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        int refPos = (int) site.getRefPos();
        if (refPos < 3) {
            return false;
        }

        int refPosCount;
        int altPosCount;
        int refNegCount;
        int altNegCount;
        
        if (site.getMajorAllele() == site.getRef()) {
            refPosCount = site.getPositiveMajorAlleleCount();
            refNegCount = site.getNegativeMajorAlleleCount();
            
        } else if (site.getMinorAllele() == site.getRef()) {
            refPosCount = site.getPositiveMinorAlleleCount();
            refNegCount = site.getNegativeMinorAlleleCount();
        } else {
            // TODO?
            return false;
        }
        altPosCount = site.getPositiveAlleleCount() - refPosCount;
        altNegCount = site.getNegativeAlleleCount() - refNegCount;
        
        String refName = site.getChrName();
        double altPosAf = (double) altPosCount / (altPosCount + refPosCount);
        double altNegAf = (double) altNegCount / (altNegCount + refNegCount);
        double altAf;
        byte base0;
        byte base1;
        byte base2;
        
        ReferenceManager referenceManager = filterEntry.getReferenceManager();
        if (altPosAf > altNegAf) {
            base2 = referenceManager.getBase(refName, refPos - 2);
            base1 = referenceManager.getBase(refName, refPos - 1);
            base0 = site.getRef();
            altAf = altPosAf;
        } else {
            base2 = getComplementaryBase(referenceManager.getBase(refName, refPos + 2));
            base1 = getComplementaryBase(referenceManager.getBase(refName, refPos + 1));
            base0 = getComplementaryBase(site.getRef());
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
        for (int i = 0; i < site.getDepth(); ++i) {
            SAMRecord r = site.getReads()[i];
            int pos = site.getBasePos()[i];
            int nextPos = tTestNegStrand ? pos - 1 : pos + 1;
            if (r.getReadNegativeStrandFlag() == tTestNegStrand &&
                nextPos < r.getReadLength() && nextPos >= 0) {
                /*
                System.out.println();
                System.out.println(r.getReadName());
                

                System.out.println(r.getReadString().substring(pos));
                System.out.println(r.getBaseQualityString().substring(pos));
                for (char c : r.getBaseQualityString().substring(pos).toCharArray()) {
                    System.out.print(((byte) c - 33) + " ");
                }
                System.out.println();
                System.out.println(r.getReadString());
                System.out.println(r.getBaseQualityString());
                
                System.out.println((char)r.getReadBases()[pos] + " " + (char) r.getReadBases()[nextPos]);
                System.out.println(r.getBaseQualities()[pos] + " " + r.getBaseQualities()[nextPos]);
                */
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
        feature[0] = base2Weight[MosaicHunterHelper.getBaseId(base2)];
        feature[1] = base1Weight[MosaicHunterHelper.getBaseId(base1)];
        feature[2] = base0Weight[MosaicHunterHelper.getBaseId(base0)];
        feature[3] = afDiff * afDiffWeight;
        feature[4] = altAf * altAfWeight;
        feature[5] = tTest * tTestWeight;
        feature[6] = intercept;
        
        double featureSum = 0;
        for (int i = 0; i < feature.length; ++i) {
            featureSum += feature[i];
        }
        
        double p = 1 / (1 + Math.exp(-featureSum));
        
        filterEntry.setMetadata(
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