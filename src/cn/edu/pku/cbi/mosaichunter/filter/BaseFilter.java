package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.StatsManager;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

abstract public class BaseFilter implements Filter {    
    
    private final String name;
    private final boolean outputFiltered;  
    private final boolean outputPassed;
    private final String outputDir;
    private FileWriter filteredWriter = null;
    private FileWriter passedWriter = null;
    private long totalSites = 0;
    private long passedSites = 0;
    private MosaicHunterContext context;
    
    public static final DecimalFormat format = new DecimalFormat("0.00000");
    
    public BaseFilter(String name) {
        this.name = name;
        outputFiltered = ConfigManager.getInstance().getBoolean(name, "output_filtered", false);
        outputPassed = ConfigManager.getInstance().getBoolean(name, "output_passed", false);
        String od = ConfigManager.getInstance().get(null, "output_dir", ".");
        if (od.trim().isEmpty()) {
            od = ".";
        }
        outputDir = od;
    }
    
    public String getName() {
        return name;
    }    

    public long getTotalSites() {
        return totalSites;
    }
    
    public long getPassedSites() {
        return passedSites;
    }
    
    public boolean validate() {
        return true;
    }
    
    public void init(MosaicHunterContext context) throws Exception {
        this.context = context;
        if (outputFiltered) {
            makeOutputDir();
            filteredWriter = new FileWriter(new File(outputDir, name + ".filtered.tsv"));
        }
        if (outputPassed) {
            makeOutputDir();
            passedWriter = new FileWriter(new File(outputDir, name + ".passed.tsv"));
        }   
    }
    
    private void makeOutputDir() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public void close() throws Exception {
        if (filteredWriter != null) {
            filteredWriter.close();
            filteredWriter = null;
        }
        if (passedWriter != null) {
            passedWriter.close();
            passedWriter = null;
        }
    }
    
    public String getOutputDir() {
        return outputDir;
    }
    
    public boolean filter(Site site) {
        StatsManager.start(name);
        boolean result = doFilter(site);
        totalSites++;
        if (result) {
            site.getPassedFilters().add(getName());
            passedSites++;
        }
        output(site, result);
        StatsManager.end(name);
        
        return result; 
    }
    
    public List<Site> filter(List<Site> sites) {
        StatsManager.start(name);
        List<Site> results = doFilter(sites);
        totalSites += sites.size();
        passedSites += results.size();
        if (passedWriter != null || filteredWriter != null) {
            Set<Site> passed = new HashSet<Site>();
            for (Site site : results) {
                output(site, true);
                passed.add(site);
                site.getPassedFilters().add(getName());
            }
            for (Site site : sites) {
                if (!passed.contains(site)) {
                    output(site, false);
                }
            }
        }
        StatsManager.end(name);
        return results; 
    }
    
    public void printStats(boolean printHeader) { 
        
        String lineFormat = "%1$-30s %2$30s %3$7s";
        if (printHeader) {
            System.out.println(String.format(lineFormat, "filter name", "pass/all", "ratio"));
        }
        System.out.println(String.format(lineFormat, 
                name, 
                passedSites + "/" + totalSites,
                String.format("%1$.2f", passedSites * 100.0 / totalSites) + "%"));
       
    }
       
    abstract public boolean doFilter(Site site);
    
    public List<Site> doFilter(List<Site> sites) {
        List<Site> results = new ArrayList<Site>();
        for (Site site : sites) {
            if (doFilter(site)) {
                results.add(site);
            }
        }
        return results;
    }
    
    public Object[] getOutputMetadata(Site site) {
        return site.getMetadata(name);
    }
    
    public String buildOutput(Site site) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(site.getRefName()).append('\t');
        sb.append(site.getRefPos()).append('\t');
        sb.append((char) site.getRef()).append('\t');
        sb.append(site.getDepth()).append('\t');
        for (int i = 0; i < site.getDepth(); ++i) {
            char base = (char) site.getBases()[i];
            if (site.getReads()[i].getReadNegativeStrandFlag()) {
                base = Character.toLowerCase(base);
            }
            sb.append(base);
        }
        sb.append('\t');
        
        for (int i = 0; i < site.getDepth(); ++i) {
            sb.append((char) (site.getBaseQualities()[i] + 33));
        }
        
        sb.append('\t');
        sb.append((char) site.getMajorAllele()).append('\t').
            append(site.getMajorAlleleCount()).append('\t');
        sb.append((char) site.getMinorAllele()).append('\t').
            append(site.getMinorAlleleCount());
        Object[] metadata = getOutputMetadata(site);
        if (metadata != null) {
            for (Object data : metadata) {
                String s;
                if (data instanceof Double) {
                    if (((Double) data).isInfinite() || ((Double) data).isNaN()) {
                        s = data.toString();
                    } else {
                        s = format.format(data);
                    }
                } else {
                    s = String.valueOf(data);
                }
               sb.append('\t').append(s);
            }
        }
        sb.append('\n');
        return sb.toString();               
    }
    
    public void output(Site site, boolean passed) {
        String outputString = null;
        if (filteredWriter != null && !passed) {
            outputString = buildOutput(site);
            try {
                filteredWriter.write(outputString);
                filteredWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (passedWriter != null && passed) {
            outputString = buildOutput(site);
            try {
                passedWriter.write(outputString);
                passedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
    }    
    
    public MosaicHunterContext getContext() {
        return context;
    }
    
}
