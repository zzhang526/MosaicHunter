package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class OutputFilter extends BaseFilter {

    private final String dataName;    
    
    public OutputFilter(String name) {
        this(name,
             ConfigManager.getInstance().get(name, "data_name", null));
    }
    
    public OutputFilter(String name, String dataName) {
        super(name);
        this.dataName = dataName;
    }
       
    @Override
    public boolean doFilter(FilterEntry filterEntry) {  
        System.out.println(filterEntry.getDepth() + " " + filterEntry.getMajorAlleleCount() + " " + filterEntry.getMinorAlleleCount());
        return true;
    }
    
    @Override
    public Object[] getOutputMetadata(FilterEntry filterEntry) {
        if (dataName == null) {
            return null;
        }
        return filterEntry.getMetadata(dataName);
    }
    
}
