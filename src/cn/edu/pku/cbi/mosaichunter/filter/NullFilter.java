package cn.edu.pku.cbi.mosaichunter.filter;

public class NullFilter extends BaseFilter {
    
    public NullFilter(String name) {
        super(name);
    }
       
    @Override
    public boolean doFilter(FilterEntry filterEntry) {  
        return false;
    }
}
