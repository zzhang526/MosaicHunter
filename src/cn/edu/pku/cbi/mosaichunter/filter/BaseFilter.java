package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.edu.pku.cbi.mosaichunter.StatsManager;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

abstract public class BaseFilter implements Filter {    
    
    private final String name;
    private final boolean outputFiltered;  
    private final boolean outputPassed;
    private final String outputDir;
    private FileWriter filteredWriter = null;
    private FileWriter passedWriter = null;
    private long entries = 0;
    private long passedEntries = 0;
    
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

    public long getEntries() {
        return entries;
    }
    
    public long getPassedEntries() {
        return passedEntries;
    }
    
    public void init() throws Exception {
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
    
    public boolean filter(FilterEntry filterEntry) {
        StatsManager.start(name);
        boolean result = doFilter(filterEntry);
        entries++;
        if (result) {
            filterEntry.getPassedFilters().add(getName());
            passedEntries++;
        }
        output(filterEntry, result);
        StatsManager.end(name);
        
        return result; 
    }
    
    public List<FilterEntry> filter(List<FilterEntry> filterEntries) {
        StatsManager.start(name);
        List<FilterEntry> results = doFilter(filterEntries);
        entries += filterEntries.size();
        passedEntries += results.size();
        if (passedWriter != null || filteredWriter != null) {
            Set<FilterEntry> passed = new HashSet<FilterEntry>();
            for (FilterEntry filterEntry : results) {
                output(filterEntry, true);
                passed.add(filterEntry);
                filterEntry.getPassedFilters().add(getName());
            }
            for (FilterEntry filterEntry : filterEntries) {
                if (!passed.contains(filterEntry)) {
                    output(filterEntry, false);
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
                passedEntries + "/" + entries,
                String.format("%1$.2f", passedEntries * 100.0 / entries) + "%"));
       
    }
       
    abstract public boolean doFilter(FilterEntry filterEntry);
    
    public List<FilterEntry> doFilter(List<FilterEntry> filterEntries) {
        List<FilterEntry> results = new ArrayList<FilterEntry>();
        for (FilterEntry entry : filterEntries) {
            if (doFilter(entry)) {
                results.add(entry);
            }
        }
        return results;
    }
    
    public Object[] getOutputMetadata(FilterEntry filterEntry) {
        return filterEntry.getMetadata(name);
    }
    
    public String buildOutput(FilterEntry filterEntry) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(filterEntry.getChrName()).append('\t');
        sb.append(filterEntry.getRefPos()).append('\t');
        sb.append((char) filterEntry.getRef()).append('\t');
        sb.append(filterEntry.getDepth()).append('\t');
        for (int i = 0; i < filterEntry.getDepth(); ++i) {
            char base = (char) filterEntry.getBases()[i];
            if (filterEntry.getReads()[i].getReadNegativeStrandFlag()) {
                base = Character.toLowerCase(base);
            }
            sb.append(base);
        }
        sb.append('\t');
        
        for (int i = 0; i < filterEntry.getDepth(); ++i) {
            sb.append((char) (filterEntry.getBaseQualities()[i] + 33));
        }
        
        sb.append('\t');
        sb.append((char) filterEntry.getMajorAllele()).append('\t').
            append(filterEntry.getMajorAlleleCount()).append('\t');
        sb.append((char) filterEntry.getMinorAllele()).append('\t').
            append(filterEntry.getMinorAlleleCount());
        Object[] metadata = getOutputMetadata(filterEntry);
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
    
    public void output(FilterEntry filterEntry, boolean passed) {
        String outputString = null;
        if (filteredWriter != null && !passed) {
            outputString = buildOutput(filterEntry);
            try {
                filteredWriter.write(outputString);
                filteredWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (passedWriter != null && passed) {
            outputString = buildOutput(filterEntry);
            try {
                passedWriter.write(outputString);
                passedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
    }    
}
