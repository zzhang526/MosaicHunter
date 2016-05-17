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

package cn.edu.pku.cbi.mosaichunter;

public class SiteObjectManager {

    private final int[] lengths;
    private final SiteFactory[] siteFactories;
    
    public SiteObjectManager(int minLength, int maxLength) {
        int n = 1;
        int l = minLength;
        while (l < maxLength) {
            n++;
            l *= 2;
        }
        lengths = new int[n];
        siteFactories = new SiteFactory[n];
        
        lengths[0] = minLength;
        for (int i = 1; i < n - 1; ++i) {
            lengths[i] = lengths[i - 1] * 2;
        }
        lengths[n - 1] = maxLength;
        
        for (int i = 0; i < n; ++i) {
            siteFactories[i] = new SiteFactory(lengths[i]);
        }
    }
    
    public void printInfo() {
        for (int i = 0; i < lengths.length; ++i) {
            System.out.println(i + " " + siteFactories[i].getName() + " " + siteFactories[i].getSize());
        }
        
        for (int i = 0; i < cnt.length; ++i) {
            if (cnt[i] > 0) {
                System.out.println(i + " " + cnt[i]);
            }
        }
    }
    static long[] cnt = new long[100];
    public Site getSite(int length) {
        if (length <= lengths[0]) {
            return siteFactories[0].getObject();
        }
        for (int i = 1; i < lengths.length; ++i) {
            if (length <= lengths[i]) {
                return siteFactories[i].getObject();
            }
        }
        StatsManager.count("site_object_manager.get.invalid_length");
        return null;
    }
    
    public void returnSite(Site site) {
        if (site == null) {
            return;
        }
        if (site.getMaxDepth()  <= lengths[0]) {
            siteFactories[0].returnObject(site);
            return;
        }
        for (int i = 1; i < lengths.length; ++i) {
            if (site.getMaxDepth() == lengths[i]) {
                siteFactories[i].returnObject(site);
                return;
            }
        }
        StatsManager.count("site_object_manager.return.invalid_length");
    } 
    

    private class SiteFactory extends ObjectFactory<Site> {
    
        private final int length;
        
        public SiteFactory(int length) {
            super("Site[" + length + "]");
            this.length = length;
        }
        
        @Override
        public Site createObject() {
            return new Site(length);
        }
    }
}
