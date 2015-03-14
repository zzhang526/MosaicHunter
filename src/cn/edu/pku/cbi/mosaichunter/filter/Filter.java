package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.List;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;

public interface Filter {

    String getName();
    
    void init(MosaicHunterContext context) throws Exception;
    
    boolean filter(Site filterEntry);
    
    List<Site> filter(List<Site> filterEntries);
    
    void printStats(boolean printHeader);
    
    void close() throws Exception;
    
    MosaicHunterContext getContext();
    
}
