package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class NearMosaicFilter extends BaseFilter {

    public static final long DEFAULT_DISTANCE = 10000;
        
    private final long distance;
    private final String auxiliaryFilterName;

    
    public NearMosaicFilter(String name) {
        this(name,
             ConfigManager.getInstance().getLong(name, "distance", DEFAULT_DISTANCE),
             ConfigManager.getInstance().get(name, "auxiliary_filter_name", null));
    }
    
    public NearMosaicFilter(String name, long distance, String auxiliaryFilterName) {
        super(name);
        this.distance = distance;
        this.auxiliaryFilterName = auxiliaryFilterName;
    }
    
    @Override
    public boolean doFilter(Site site) {
        return true;
    }
    
    @Override
    public List<Site> doFilter(List<Site> sites) {
        List<Site> results = new ArrayList<Site>();
        List<Site> pendingSites = new ArrayList<Site>();
        
        String lastChrName = "";
        long lastMosaicPos = -1;
        for (Site site : sites) {
            String chrName = site.getRefName();
            if (!chrName.equals(lastChrName)) {
                pendingSites = new ArrayList<Site>();
                lastMosaicPos = -1;
                lastChrName = chrName;
            }      
            if (auxiliaryFilterName == null || 
                !site.getPassedFilters().contains(auxiliaryFilterName)) {
                // add sites before current mosaic site
                for (Site pendingSite : pendingSites) {
                    if (site.getRefPos() - pendingSite.getRefPos() <= distance) {
                        pendingSite.setMetadata(
                                getName(), 
                                new Object[] {pendingSite.getRefPos() - site.getRefPos()});
                        results.add(pendingSite);
                    }
                }
                // add current mosaic site
                site.setMetadata(getName(), new Object[] {0L});
                results.add(site);
                
                lastMosaicPos = site.getRefPos();
                pendingSites = new ArrayList<Site>();
            } else if (lastMosaicPos > -1 && site.getRefPos() - lastMosaicPos <= distance) {
                // add site after last mosaic site
                site.setMetadata(getName(), new Object[] {site.getRefPos() - lastMosaicPos});
                results.add(site);
            } else {
                pendingSites.add(site);
            }
            
        }
        return results;   
    }

}
