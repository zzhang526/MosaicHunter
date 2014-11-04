package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class DepthFilter extends BaseFilter {

    public static final int DEFAULT_MIN_DEPTH = 25;
    public static final int DEFAULT_MAX_DEPTH = 150;
    
    private final int minDepth;
    private final int maxDepth;
    
    public DepthFilter(String name) {
        this(name,
             ConfigManager.getInstance().getInt(name, "min_depth", DEFAULT_MIN_DEPTH),
             ConfigManager.getInstance().getInt(name, "max_depth", DEFAULT_MAX_DEPTH));
    }
    
    public DepthFilter(String name, int minDepth, int maxDepth) {
        super(name);
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {  
        return filterEntry.getDepth() >= minDepth && filterEntry.getDepth() <= maxDepth;
    }
    
}
