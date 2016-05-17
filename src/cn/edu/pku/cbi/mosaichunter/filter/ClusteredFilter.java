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

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class ClusteredFilter extends BaseFilter {

    public static final long DEFAULT_INNER_DISTANCE = 20000;
    public static final long DEFAULT_OUTER_DISTANCE = 20000;
        
    private final long innerDistance;
    private final long outerDistance;
    private final String auxiliaryFilterName;
    
    public ClusteredFilter(String name) {
        this(name,
             ConfigManager.getInstance().getLong(name, "inner_distance", DEFAULT_INNER_DISTANCE),
             ConfigManager.getInstance().getLong(name, "outer_distance", DEFAULT_OUTER_DISTANCE),
             ConfigManager.getInstance().get(name, "auxiliary_filter_name", null));
    }
    
    public ClusteredFilter(
            String name, long innerDistance, long outerDistance, String auxiliaryFilterName) {
        super(name);
        this.innerDistance = innerDistance;
        this.outerDistance = outerDistance;
        this.auxiliaryFilterName = auxiliaryFilterName;
    }
    
    @Override
    public boolean doFilter(Site site) {
        return true;
    }
    
    @Override
    public List<Site> doFilter(List<Site> sites) {
        List<Site> filtered = new ArrayList<Site>();
        ArrayList<Site> pendingSites = new ArrayList<Site>();
        String lastChr = null;
        for (Site site : sites) {
            if (!site.getRefName().equals(lastChr)) {
                filtered.addAll(process(pendingSites));
                lastChr = site.getRefName();
                pendingSites = new ArrayList<Site>();
            
            }
            pendingSites.add(site);
        }
        filtered.addAll(process(pendingSites));
        return filtered;
    }
    
    public List<Site> process(List<Site> sites) {
        int n = sites.size();
        if (n < 3) {
            List<Site> passed = new ArrayList<Site>();
            for (Site site : sites) {
                if (auxiliaryFilterName == null ||
                    !site.getPassedFilters().contains(auxiliaryFilterName)) {
                    passed.add(site);
                }
            }
            return passed;
        }
        boolean[] filterFlag = new boolean[n]; 
        
        Site site0 = null;
        Site site1 = sites.get(0);
        Site site2 = sites.get(1);
        Site site3 = null;
        
        for (int i = 2; i < n; ++i) {
            site3 = sites.get(i);
            if (site3.getRefName().equals(site1.getRefName()) && 
                site3.getRefPos() - site1.getRefPos() <= innerDistance) {
                filterFlag[i - 2] = true;
                filterFlag[i - 1] = true;
                filterFlag[i] = true;
                if (site0 != null && 
                    site0.getRefName().equals(site1.getRefName()) && 
                    site1.getRefPos() - site0.getRefPos() <= outerDistance) {
                    filterFlag[i - 3] = true;
                }
                if (i + 1 < n &&
                    sites.get(i + 1).getRefName().equals(site3.getRefName()) &&
                    sites.get(i + 1).getRefPos() - site3.getRefPos() <= outerDistance) {
                    filterFlag[i + 1] = true;
                }
            }
            site0 = site1;
            site1 = site2;
            site2 = site3;
        }
        
        List<Site> passed = new ArrayList<Site>();
        for (int i = 0; i < n; ++i) {
            Site site = sites.get(i);
            String posBefore = "";
            for (int k = i - 3; k < i; ++k) {
                if (k > 0) {
                    Site siteBefore = sites.get(k);
                    if (siteBefore.getRefName().equals(site.getRefName())) {
                        if (!posBefore.isEmpty()) {
                            posBefore += ",";
                        }
                        posBefore += siteBefore.getRefPos();
                    }
                }
            }
            String posAfter = "";
            for (int k = i + 1; k <= i + 3; ++k) {
                if (k < n) {
                    Site siteAfter = sites.get(k);
                    if (siteAfter.getRefName().equals(site.getRefName())) {
                        if (!posAfter.isEmpty()) {
                            posAfter += ",";
                        }
                        posAfter += siteAfter.getRefPos();
                    }
                }
            }
            
            site.setMetadata(
                    getName(),
                    new Object[] {
                        posBefore,
                        posAfter});
            if (!filterFlag[i] && 
                (auxiliaryFilterName == null || 
                !site.getPassedFilters().contains(auxiliaryFilterName))) {
                passed.add(site);
            } 
        }
        return passed;
    }

}
