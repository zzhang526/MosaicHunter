package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.List;

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
    public void init() throws Exception {
        super.init();
        for (Filter filter : filters) {
            filter.init();
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
    public boolean doFilter(FilterEntry filterEntry) {        
        for (Filter filter : filters) {
            boolean result = filter.filter(filterEntry);
            if (!result) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public List<FilterEntry> doFilter(List<FilterEntry> filterEntries) {
        for (Filter filter : filters) {
            filterEntries = filter.filter(filterEntries);
        }
        return filterEntries;
    }    
}
