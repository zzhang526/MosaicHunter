package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.config.Validator;

public class RegionFilter extends BaseFilter {

    public static final int DEFAULT_EXPANSION = 5;
    public static final boolean DEFAULT_INCLUDE = false;
    
    private final String bedFile;
    private final int expansion;
    private final boolean include;
    
    private final Map<String, List<Region>> regions = new HashMap<String, List<Region>>();
    
    public RegionFilter(String name) {
        this(name,
             ConfigManager.getInstance().get(name, "bed_file", null),
             ConfigManager.getInstance().getInt(name, "expansion", DEFAULT_EXPANSION),
             ConfigManager.getInstance().getBoolean(name, "include", DEFAULT_INCLUDE));
    }
    
    public RegionFilter(String name, String bedFile, int expansion, boolean include) {
        super(name);
        this.bedFile = bedFile;
        this.expansion = expansion;
        this.include = include;
        
    }
    
    @Override
    public boolean validate() {
        boolean ok = true;
        if (!Validator.validateFileExists(
                getName() + ".bed_file", bedFile, false)) {
            ok = false;     
        }
        return ok;
    }
    
    
    @Override
    public void init(MosaicHunterContext context) throws Exception {
        super.init(context);
        if (bedFile == null || bedFile.isEmpty()) {
            return;
        }
        BufferedReader reader = null; 
        try {
            reader = new BufferedReader(new FileReader(bedFile));
            for(;;) {
                String line =  reader.readLine();
               if (line == null) {
                   break;
               }
               String[] tokens = line.split("\\t");
               if (tokens.length < 3) {
                   continue;
               }
               String chrName = tokens[0];
               Region region = new Region(
                       Long.parseLong(tokens[1]) + 1 - expansion,
                       Long.parseLong(tokens[2]) + expansion);
               if (region.start < 0) {
                   region.start = 0;
               }
               if (region.end < region.start) {
                   continue;
               }
               List<Region> r = regions.get(chrName);
               if (r == null) {
                   r = new ArrayList<Region>();
                   regions.put(chrName, r);
               }
               r.add(region);
        
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }    
    }
    
    @Override
    public boolean doFilter(Site site) { 
        boolean inRegion = false;
        List<Region> r = regions.get(site.getRefName());
        if (r != null) {
            Region pos = new Region(site.getRefPos(), site.getRefPos());        
            int index = Collections.binarySearch(r, pos);
            if (index < 0) {
                index = -index - 2;
            }
            if (index >= 0) {   
                Region region = r.get(index);
                inRegion = (region.end >= site.getRefPos());
                if (inRegion) {
                    site.setMetadata(
                            getName(),
                            new Object[] {
                                site.getRefName(),
                                region.start,
                                region.end});
                }
            }
        }
        return inRegion == include;
    }
    
    private class Region implements Comparable<Region> {
        private long start;
        private long end;
        
        public Region(long start, long end) {
            this.start = start;
            this.end = end;
        }
        
        public int compareTo(Region that) {
            if (this.start == that.start) {
                return 0;
            } else if (this.start < that.start) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
