package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.HashSet;
import java.util.Set;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class PositionFilter extends BaseFilter {

    
    private final Set<String> positions = new HashSet<String>();
    
    public PositionFilter(String name) {
        this(name,
             ConfigManager.getInstance().getValues(
                     name, "positions", new String[0]));
    }
    
    public PositionFilter(String name, String[] positions) {
        super(name);
        for (String position : positions) {
            this.positions.add(position);
        }
    }
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {
        return positions.contains(filterEntry.getChrName() + ":" + filterEntry.getRefPos());
    }
    
}
