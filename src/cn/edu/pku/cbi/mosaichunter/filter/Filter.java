package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.List;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;

public interface Filter {

    String getName();
    
    boolean validate();
    
    void init(MosaicHunterContext context) throws Exception;
    
    boolean filter(Site site);
    
    List<Site> filter(List<Site> sites);
    
    void printStats(boolean printHeader);
    
    void close() throws Exception;
    
    MosaicHunterContext getContext();
    
}
