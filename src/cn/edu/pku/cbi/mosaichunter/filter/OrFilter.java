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

import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class OrFilter extends BaseFilter {

    
    private final Filter[] filters;
    
    public OrFilter(String name) throws Exception {
        this(name, FilterFactory.create(
                ConfigManager.getInstance().getValues(name, "filters", new String[0])));  
    }
    
    public OrFilter(String name, Filter... filters) {
        super(name);
        this.filters = filters;
    }
    
    @Override
    public boolean validate() {
        boolean ok = super.validate();
        for (Filter filter : filters) {
            ok &= filter.validate();
        }
        return ok;
    }
    
    @Override
    public void init(MosaicHunterContext context) throws Exception {
        super.init(context);
        for (Filter filter : filters) {
            filter.init(context);
        }
    }
    
    @Override
    public void close() throws Exception {
        for (Filter filter : filters) {
            filter.close();
        }
    }
    
    public Filter[] getFilters() {
        return filters;
    }
    
    @Override
    public void printStats(boolean printHeader) { 
        boolean first = true;
        for (Filter filter : filters) {
            filter.printStats(printHeader && first);
            first = false;
        }
    }    
    
    @Override
    public boolean doFilter(Site site) {        
        for (Filter filter : filters) {
            boolean result = filter.filter(site);
            if (result) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<Site> doFilter(List<Site> sites) {
        List<Site> results = new ArrayList<Site>();
        for (Site site : sites) {
            if (doFilter(site)) {
                results.add(site);
            }
        }
        return results;
    }    
}
