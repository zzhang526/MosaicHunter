package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class ExomeParameterFilter extends BaseFilter {
    
    
    public static final int DEFAULT_MIN_GROUP_SIZE = 50;
    public static final int DEFAULT_OPTIMAL_DEPTH = 80;
    
    private final int minGroupSize;
    private final int optimalDepth;
    private final String rDataFile;
    private FileWriter rDataWriter = null;

    
    private static final List<SimpleSite> sites = new ArrayList<SimpleSite>();
    
    public ExomeParameterFilter(String name) {
        this(name,
             ConfigManager.getInstance().getInt(name, "min_group_size", DEFAULT_MIN_GROUP_SIZE),
             ConfigManager.getInstance().getInt(name, "optimal_depth", DEFAULT_OPTIMAL_DEPTH),
             ConfigManager.getInstance().get(name, "r_data_file", "r_het_data.tsv"));
    }
    
    public ExomeParameterFilter(String name, 
            int minGroupSize, int optimalDepth, String rDataFile) {
        super(name);
        this.minGroupSize = minGroupSize;
        this.optimalDepth = optimalDepth;
        this.rDataFile = rDataFile;
    }        
    
    @Override
    public void init(MosaicHunterContext context) throws Exception {
        super.init(context);
        if (rDataFile != null) {
            rDataWriter = new FileWriter(new File(this.getOutputDir(), rDataFile));
        }
    }
    
    @Override
    public boolean doFilter(Site site) {
        char refAllel;
        char altAllel;
        int ref = 0;
        int alt = 0;
        int depth = site.getMajorAlleleCount() + site.getMinorAlleleCount();
        if (site.getRef() == site.getMajorAllele()) {
            ref = site.getMajorAlleleCount();
            alt = site.getMinorAlleleCount();
            refAllel = (char) site.getMajorAllele();
            altAllel = (char) site.getMinorAllele();
            } else if (site.getRef() == site.getMinorAllele()) {
            ref = site.getMinorAlleleCount();
            alt = site.getMajorAlleleCount();
            refAllel = (char) site.getMinorAllele();
            altAllel = (char) site.getMajorAllele();
        } else {
            return false;
        }
        
        double altAf = (double) alt / depth;
        sites.add(new SimpleSite(depth, altAf));
        site.setMetadata(
                getName(),
                new Object[] {altAf});
        StringBuilder sb = new StringBuilder();
        sb.append(site.getRefName()).append('\t')
          .append(site.getRefPos()).append('\t')
          .append(refAllel).append('\t')
          .append(altAllel).append('\t')
          .append(ref).append('\t')
          .append(alt).append('\t')
          .append(-(Double) site.getMetadata("heterozygous_filter")[7]).append('\t')
          .append(-(Double) site.getMetadata("heterozygous_filter")[8]).append('\t')
          .append(-(Double) site.getMetadata("heterozygous_filter")[9]).append('\t')
          .append(0).append('\t')
          .append(alt + ref).append('\n');
        
        try {
            rDataWriter.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
          
        return true;
        
    }
    
    @Override
    public void close() throws IOException {
        String in = null;
        
        //in = "data/exome_parameter_filter.passed.tsv";
        //in = "data/shu_family.64.s7.tsv";
      
        if (in != null) {
            sites.clear();
            List<String[]> f = null;
            try {
                f = MosaicHunterHelper.readTsvFile(in);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            for (int i = 0; i < f.size(); ++i) {
                String[] ff = f.get(i);   
                if (ff.length >= 11) {
                    int ref = Integer.parseInt(ff[7]);
                    int alt = Integer.parseInt(ff[9]);
                    sites.add(new SimpleSite(ref + alt, Double.parseDouble(ff[ff.length - 1])));
                } else {
                    double p = Double.parseDouble(ff[7]);
                    if (Math.pow(10, -p) < 0.9) {
                        continue;
                    }
                    int ref = Integer.parseInt(ff[4]);
                    int alt = Integer.parseInt(ff[5]);
                    sites.add(new SimpleSite(ref + alt, (double) alt / (ref + alt)));
                }
            }
        }
        
        Collections.sort(sites, new Comparator<SimpleSite>() {
            public int compare(SimpleSite a, SimpleSite b) {
                if (a.depth > b.depth) {
                    return 1;
                } else if (a.depth < b.depth) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        
        int n = sites.size();
        if (n == 0) {
            System.out.println("No data.");
            return;
        }
        int[] depth = new int[n];
        long totalDepth = 0;
        double[] af = new double[n];
        for (int i = 0; i < n; ++i) {
            depth[i] = sites.get(i).depth;
            af[i] = sites.get(i).altAf;  
            totalDepth += depth[i];
        }
        
        int maxGroups = n / minGroupSize + 2;
        int m = 0;
        double afMeanAll = 0;
        
        int[] groupSize = new int[maxGroups];
        int[] groupPos = new int[maxGroups];
        double[] afMean = new double[maxGroups];
        double[] depthMid = new double[maxGroups];
        double[] depthMidR = new double[maxGroups];
        double[] afSd = new double[maxGroups];
        double[] afSd2 = new double[maxGroups];
        for (int i = 0; i <= n; ++i) {
            if (i == n || (i > 0 && depth[i] != depth[i - 1] && groupSize[m] >= minGroupSize)) {
                afMean[m] /= groupSize[m];
                for (int j = groupPos[m]; j < i; ++j) {
                    afSd2[m] += (af[j] - afMean[m]) * (af[j] - afMean[m]);
                }
                if (groupSize[m] > 1) {
                    afSd2[m] /= groupSize[m] - 1;
                }
                afSd[m] = Math.sqrt(afSd2[m]);
                depthMid[m] = depth[(i + groupPos[m]) / 2];
                depthMidR[m] = 1.0 / depthMid[m];
                m++;
                if (i == n) {
                    break;
                }
                groupPos[m] = i;
            }
            afMeanAll += af[i];
            groupSize[m]++;
            afMean[m] += af[i];
        }
        afMeanAll /= n;
         
        SimpleRegression sr = new SimpleRegression();
        for (int i = 0; i < m; ++i) {
            sr.addData(depthMidR[i], afSd2[i]);
        }

        
        double k = sr.getSlope();
        double d = sr.getIntercept();
        
        
        //System.out.println("mean: " + afMeanAll);
        //System.out.println("slop: " + k);
        //System.out.println("intercept: " + d);
  
        
        double v = k / optimalDepth + d - afMeanAll * (1 - afMeanAll) / optimalDepth;
        double alpha = ((1 - afMeanAll) / v - 1 / afMeanAll) * afMeanAll * afMeanAll;
        double beta =  alpha * (1 / afMeanAll - 1);
        
        //System.out.println("alpha: " + alpha);
        //System.out.println("beta: " + beta);
        
        long averageDepth = n == 0 ? 0 : totalDepth / n;
        System.out.println("average depth: " + averageDepth);
        System.out.println("alpha: " + Math.round(alpha));
        System.out.println("beta: " + Math.round(beta));
        
        if (rDataWriter != null) {
            rDataWriter.close();
        }
    }   
    
    private class SimpleSite {
        private final int depth;
        private final double altAf;
        public SimpleSite(int depth, double altAf) {
            this.depth = depth;
            this.altAf = altAf;
        }
    }
}
