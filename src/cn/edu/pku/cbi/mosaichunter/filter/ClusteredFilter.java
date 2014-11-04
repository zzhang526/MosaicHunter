package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class ClusteredFilter extends BaseFilter {

    public static final long DEFAULT_INNER_DISTANCE = 20000;
    public static final long DEFAULT_OUTER_DISTANCE = 20000;
        
    private final long innerDistance;
    private final long outerDistance;
    
    public ClusteredFilter(String name) {
        this(name,
             ConfigManager.getInstance().getLong(name, "inner_distance", DEFAULT_INNER_DISTANCE),
             ConfigManager.getInstance().getLong(name, "outer_distance", DEFAULT_OUTER_DISTANCE));
    }
    
    public ClusteredFilter(String name, long innerDistance, long outerDistance) {
        super(name);
        this.innerDistance = innerDistance;
        this.outerDistance = outerDistance;
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {
        return true;
    }
    
    @Override
    public List<FilterEntry> doFilter(List<FilterEntry> filterEntries) {
        //System.out.println(new Date() + " " + getName() + " " + filterEntries.size());        
        List<FilterEntry> filtered = new ArrayList<FilterEntry>();
        ArrayList<FilterEntry> entries = new ArrayList<FilterEntry>();
        String lastChr = null;
        for (FilterEntry entry : filterEntries) {
            if (!entry.getChrName().equals(lastChr)) {
                filtered.addAll(process(entries));
                lastChr = entry.getChrName();
                entries = new ArrayList<FilterEntry>();
            
            }
            entries.add(entry);
        }
        filtered.addAll(process(entries));
        return filtered;
    }
    
    public List<FilterEntry> process(List<FilterEntry> filterEntries) {
        int n = filterEntries.size();
        if (n < 3) {
            List<FilterEntry> passed = new ArrayList<FilterEntry>();
            for (FilterEntry entry : filterEntries) {
                if (!entry.getPassedFilters().contains("heterozygous_filter")) {
                    passed.add(entry);
                }
            }
            return passed;
        }
        boolean[] filterFlag = new boolean[n]; 
        
        FilterEntry entry0 = null;
        FilterEntry entry1 = filterEntries.get(0);
        FilterEntry entry2 = filterEntries.get(1);
        FilterEntry entry3 = null;
        
        for (int i = 2; i < n; ++i) {
            entry3 = filterEntries.get(i);
            if (entry3.getChrName().equals(entry1.getChrName()) && 
                entry3.getRefPos() - entry1.getRefPos() <= innerDistance) {
                filterFlag[i - 2] = true;
                filterFlag[i - 1] = true;
                filterFlag[i] = true;
                if (entry0 != null && 
                    entry0.getChrName().equals(entry1.getChrName()) && 
                    entry1.getRefPos() - entry0.getRefPos() <= outerDistance) {
                    filterFlag[i - 3] = true;
                }
                if (i + 1 < n &&
                    filterEntries.get(i + 1).getChrName().equals(entry3.getChrName()) &&
                    filterEntries.get(i + 1).getRefPos() - entry3.getRefPos() <= outerDistance) {
                    filterFlag[i + 1] = true;
                }
            }
            entry0 = entry1;
            entry1 = entry2;
            entry2 = entry3;
        }
        
        List<FilterEntry> passed = new ArrayList<FilterEntry>();
        for (int i = 0; i < n; ++i) {
            FilterEntry entry = filterEntries.get(i);
            String posBefore = "";
            for (int k = i - 3; k < i; ++k) {
                if (k > 0) {
                    FilterEntry entryBefore = filterEntries.get(k);
                    if (entryBefore.getChrName().equals(entry.getChrName())) {
                        if (!posBefore.isEmpty()) {
                            posBefore += ",";
                        }
                        posBefore += entryBefore.getRefPos();
                    }
                }
            }
            String posAfter = "";
            for (int k = i + 1; k <= i + 3; ++k) {
                if (k < n) {
                    FilterEntry entryAfter = filterEntries.get(k);
                    if (entryAfter.getChrName().equals(entry.getChrName())) {
                        if (!posAfter.isEmpty()) {
                            posAfter += ",";
                        }
                        posAfter += entryAfter.getRefPos();
                    }
                }
            }
            
            entry.setMetadata(
                    getName(),
                    new Object[] {
                        posBefore,
                        posAfter});
            if (!filterFlag[i] && !entry.getPassedFilters().contains("heterozygous_filter")) {
                passed.add(entry);
            } 
        }
        return passed;
    }

}
