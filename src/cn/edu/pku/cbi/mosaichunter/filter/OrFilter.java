package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class OrFilter extends BaseFilter {

    
    private final Filter[] filters;
    
    public OrFilter(String name) throws Exception {
        this(name, FilterFactory.create(
                ConfigManager.getInstance().getValues(name, "filters", new String[0])));  
    }
    
    public OrFilter(String name, Filter... filters) {
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
    public boolean doFilter(Site filterEntry) {        
        for (Filter filter : filters) {
            boolean result = filter.filter(filterEntry);
            if (result) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<Site> doFilter(List<Site> filterEntries) {
        List<Site> results = new ArrayList<Site>();
        for (Site entry : filterEntries) {
            if (doFilter(entry)) {
                results.add(entry);
            }
        }
        return results;
    }    
}
