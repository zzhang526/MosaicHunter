package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class NearMosaicFilter extends BaseFilter {

    public static final long DEFAULT_DISTANCE = 10000;
        
    private final long distance;
    
    public NearMosaicFilter(String name) {
        this(name,
             ConfigManager.getInstance().getLong(name, "distance", DEFAULT_DISTANCE));
    }
    
    public NearMosaicFilter(String name, long distance) {
        super(name);
        this.distance = distance;
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {
        return true;
    }
    
    @Override
    public List<FilterEntry> doFilter(List<FilterEntry> filterEntries) {
        List<FilterEntry> results = new ArrayList<FilterEntry>();
        List<FilterEntry> pendingEntries = new ArrayList<FilterEntry>();
        
        String lastChrName = "";
        long lastMosaicPos = -1;
        for (FilterEntry entry : filterEntries) {
            String chrName = entry.getChrName();
            if (!chrName.equals(lastChrName)) {
                pendingEntries = new ArrayList<FilterEntry>();
                lastMosaicPos = -1;
                lastChrName = chrName;
            }      
           
            if (!entry.getPassedFilters().contains("heterozygous_filter")) {
                // add sites before current mosaic site
                for (FilterEntry pendingEntry : pendingEntries) {
                    if (entry.getRefPos() - pendingEntry.getRefPos() <= distance) {
                        pendingEntry.setMetadata(
                                getName(), 
                                new Object[] {pendingEntry.getRefPos() - entry.getRefPos()});
                        results.add(pendingEntry);
                    }
                }
                // add current mosaic site
                entry.setMetadata(getName(), new Object[] {0L});
                results.add(entry);
                
                lastMosaicPos = entry.getRefPos();
                pendingEntries = new ArrayList<FilterEntry>();
            } else if (lastMosaicPos > -1 && entry.getRefPos() - lastMosaicPos <= distance) {
                // add site after last mosaic site
                entry.setMetadata(getName(), new Object[] {entry.getRefPos() - lastMosaicPos});
                results.add(entry);
            } else {
                pendingEntries.add(entry);
            }
            
        }
        return results;   
    }

}
