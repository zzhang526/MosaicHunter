package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.cbi.mosaichunter.Site;
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
    public boolean doFilter(Site filterEntry) {
        return true;
    }
    
    @Override
    public List<Site> doFilter(List<Site> filterEntries) {
        //System.out.println(new Date() + " " + getName() + " " + filterEntries.size());        
        List<Site> filtered = new ArrayList<Site>();
        ArrayList<Site> entries = new ArrayList<Site>();
        String lastChr = null;
        for (Site entry : filterEntries) {
            if (!entry.getRefName().equals(lastChr)) {
                filtered.addAll(process(entries));
                lastChr = entry.getRefName();
                entries = new ArrayList<Site>();
            
            }
            entries.add(entry);
        }
        filtered.addAll(process(entries));
        return filtered;
    }
    
    public List<Site> process(List<Site> filterEntries) {
        int n = filterEntries.size();
        if (n < 3) {
            List<Site> passed = new ArrayList<Site>();
            for (Site entry : filterEntries) {
                if (!entry.getPassedFilters().contains("mosaic_like_filter")) {
                    passed.add(entry);
                }
            }
            return passed;
        }
        boolean[] filterFlag = new boolean[n]; 
        
        Site entry0 = null;
        Site entry1 = filterEntries.get(0);
        Site entry2 = filterEntries.get(1);
        Site entry3 = null;
        
        for (int i = 2; i < n; ++i) {
            entry3 = filterEntries.get(i);
            if (entry3.getRefName().equals(entry1.getRefName()) && 
                entry3.getRefPos() - entry1.getRefPos() <= innerDistance) {
                filterFlag[i - 2] = true;
                filterFlag[i - 1] = true;
                filterFlag[i] = true;
                if (entry0 != null && 
                    entry0.getRefName().equals(entry1.getRefName()) && 
                    entry1.getRefPos() - entry0.getRefPos() <= outerDistance) {
                    filterFlag[i - 3] = true;
                }
                if (i + 1 < n &&
                    filterEntries.get(i + 1).getRefName().equals(entry3.getRefName()) &&
                    filterEntries.get(i + 1).getRefPos() - entry3.getRefPos() <= outerDistance) {
                    filterFlag[i + 1] = true;
                }
            }
            entry0 = entry1;
            entry1 = entry2;
            entry2 = entry3;
        }
        
        List<Site> passed = new ArrayList<Site>();
        for (int i = 0; i < n; ++i) {
            Site entry = filterEntries.get(i);
            String posBefore = "";
            for (int k = i - 3; k < i; ++k) {
                if (k > 0) {
                    Site entryBefore = filterEntries.get(k);
                    if (entryBefore.getRefName().equals(entry.getRefName())) {
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
                    Site entryAfter = filterEntries.get(k);
                    if (entryAfter.getRefName().equals(entry.getRefName())) {
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
            if (!filterFlag[i] && !entry.getPassedFilters().contains("mosaic_like_filter")) {
                passed.add(entry);
            } 
        }
        return passed;
    }

}
