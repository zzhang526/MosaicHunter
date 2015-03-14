package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class NullFilter extends BaseFilter {
    
    public static final boolean DEFAULT_RETURN_VALUE = false;

    private final boolean returnValue;
    
    private int[] x = new int[100];
    public NullFilter(String name) {
        this(name, 
             ConfigManager.getInstance().getBoolean(name, "return_value", DEFAULT_RETURN_VALUE));
    }
       
    public NullFilter(String name, boolean returnValue) {
        super(name);
        this.returnValue = returnValue;
    }
    
    
    @Override
    public boolean doFilter(Site filterEntry) {  
        x[0] |= filterEntry.getDepth();
        if (true || filterEntry.getRefPos() % 11 == 0) {
        for (int i = 0; i < filterEntry.getDepth(); ++i) {
            x[1] ^= filterEntry.getBases()[i];
            x[2] ^= filterEntry.getBaseQualities()[i];
            x[3] ^= filterEntry.getBasePos()[i];
        }
        
        x[4] ^= filterEntry.getAlleleCount((byte)'A');
        x[5] ^= filterEntry.getAlleleCount((byte)'C');
        x[6] ^= filterEntry.getAlleleCount((byte)'G');
        x[7] ^= filterEntry.getAlleleCount((byte)'T');
        
        x[8] ^= filterEntry.getMajorAlleleCount();
        x[9] ^= filterEntry.getMinorAlleleCount();
        x[10] ^= filterEntry.getNegativeAlleleCount();
      
        x[11] ^= filterEntry.getRefName().hashCode();
        x[12] ^= filterEntry.getRef();
        x[13] ^= filterEntry.getMajorAllele();
        x[14] ^= filterEntry.getMinorAllele();
        }
        /*
        for (int i =0; i < filterEntry.getDepth(); ++i) {
            System.out.println(filterEntry.getReads()[i].getReadName() + " " +
        filterEntry.getReads()[i].getAlignmentStart() + " " + i
        + " " + filterEntry.getReads()[i].getCigarString());
        }
        
            */
        return returnValue;
    }
    @Override
    public void close() {
        long xx = 0;
        for (int i = 0; i < x.length; ++i) {
            if (x[i] > 0) {
                xx ^= x[i];
                System.out.println(i + " " + x[i]);
            }
        }
        System.out.println("final " + xx);

    }
}
