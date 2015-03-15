package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class NullFilter extends BaseFilter {
    
    public static final boolean DEFAULT_RETURN_VALUE = false;

    private final boolean returnValue;
    
    public NullFilter(String name) {
        this(name, 
             ConfigManager.getInstance().getBoolean(name, "return_value", DEFAULT_RETURN_VALUE));
    }
       
    public NullFilter(String name, boolean returnValue) {
        super(name);
        this.returnValue = returnValue;
    }
    
    @Override
    public boolean doFilter(Site site) {  
        return returnValue;
    }
    
}
