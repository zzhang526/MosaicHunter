package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class BlacklistFilter extends BaseFilter {

    private final String blacklistFile;
    
    private final Set<String> blacklist = new HashSet<String>();
    
    public BlacklistFilter(String name) {
        this(name,
             ConfigManager.getInstance().get(name, "blacklist_file", null));
    }
    
    public BlacklistFilter(String name, String blacklistFile) {
        super(name);
        this.blacklistFile = blacklistFile;        
    }
    
    
    @Override
    public void init(MosaicHunterContext context) throws Exception {
        super.init(context);
        if (blacklistFile == null || blacklistFile.trim().isEmpty()) {
            return;
        }
        BufferedReader reader = null; 
        try {
            reader = new BufferedReader(new FileReader(blacklistFile));
            for(;;) {
                String line =  reader.readLine();
               if (line == null) {
                   break;
               }
               String[] tokens = line.split("\\t");
               if (tokens.length < 2) {
                   continue;
               }
               String key = tokens[0] + ":" + tokens[1];
               blacklist.add(key);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }        
    }
    
    @Override
    public boolean doFilter(Site site) { 
        return !blacklist.contains(site.getRefName() + ":" + site.getRefPos());
    }
}
