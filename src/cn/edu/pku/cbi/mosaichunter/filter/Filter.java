package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.List;

public interface Filter {

    String getName();
    
    void init() throws Exception;
    
    boolean filter(FilterEntry filterEntry);
    
    List<FilterEntry> filter(List<FilterEntry> filterEntries);
    
    void printStats(boolean printHeader);
    
    void close() throws Exception;
    
}
