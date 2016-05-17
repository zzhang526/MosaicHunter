/*
 * The MIT License
 *
 * Copyright (c) 2016 Center for Bioinformatics, Peking University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
