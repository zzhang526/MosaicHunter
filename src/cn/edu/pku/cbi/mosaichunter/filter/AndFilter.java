package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.List;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class AndFilter extends BaseFilter {

    
    private final Filter[] filters;
    
    public AndFilter(String name) throws Exception {
        this(name, FilterFactory.create(
                ConfigManager.getInstance().getValues(name, "filters", new String[0])));  
    }
    
    public AndFilter(String name, Filter... filters) {
        super(name);
        this.filters = filters;
    }
    
    @Override
    public void init(MosaicHunterContext context) throws Exception {
        super.init(context);
        for (Filter filter : filters) {
            filter.init(context);
        }
    }
    
    @Override
    public void close() throws Exception {
        for (Filter filter : filters) {
            filter.close();
        }
    }
    
    public Filter[] getFilters() {
        return filters;
    }
    
    @Override
    public void printStats(boolean printHeader) { 
        boolean first = true;
        for (Filter filter : filters) {
            filter.printStats(printHeader && first);
            first = false;
        }
    }    
    
    @Override
    public boolean doFilter(Site site) {        
        for (Filter filter : filters) {
            boolean result = filter.filter(site);
            if (!result) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public List<Site> doFilter(List<Site> sites) {
        for (Filter filter : filters) {
            sites = filter.filter(sites);
        }
        return sites;
    }    
}
