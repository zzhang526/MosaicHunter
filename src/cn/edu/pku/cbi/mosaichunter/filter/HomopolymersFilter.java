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

import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class HomopolymersFilter extends BaseFilter {

    public static final int DEFAULT_SHORT_HOMOPOLYMER_LENGTH = 4;
    public static final int DEFAULT_LONG_HOMOPOLYMER_LENGTH = 6;
    public static final int DEFAULT_SHORT_HOMOPOLYMER_EXPANSION = 2;
    public static final int DEFAULT_LONG_HOMOPOLYMER_EXPANSION = 3;
    
    private final int shortHomopolymerLength;
    private final int longHomopolymerLength;
    private final int shortHomopolymerExpansion;
    private final int longHomopolymerExpansion;
    
    public HomopolymersFilter(String name) {
        this(name,
             ConfigManager.getInstance().get(null, "reference_file"),
             ConfigManager.getInstance().getInt(
                     name, "short_homopolymer_length", DEFAULT_SHORT_HOMOPOLYMER_LENGTH),
             ConfigManager.getInstance().getInt(
                     name, "long_homopolymer_length", DEFAULT_LONG_HOMOPOLYMER_LENGTH),
             ConfigManager.getInstance().getInt(
                     name, "short_homopolymer_expansion", DEFAULT_SHORT_HOMOPOLYMER_EXPANSION),
             ConfigManager.getInstance().getInt(
                     name, "long_homopolymer_expansion", DEFAULT_LONG_HOMOPOLYMER_EXPANSION));
    }
    
    public HomopolymersFilter(String name, String referenceFile,
            int shortHomopolymerLength, int longHomopolymerLength, 
            int shortHomopolymerExpansion, int longHomopolymerExpansion) {
        super(name);
        this.shortHomopolymerLength = shortHomopolymerLength;
        this.longHomopolymerLength = longHomopolymerLength;
        this.shortHomopolymerExpansion = shortHomopolymerExpansion;
        this.longHomopolymerExpansion = longHomopolymerExpansion;
    }
    
    @Override
    public boolean doFilter(Site site) {   
        
        long pos = site.getRefPos();
        int width = Math.max(
                longHomopolymerLength + longHomopolymerExpansion, 
                shortHomopolymerLength + shortHomopolymerExpansion);
        
        long left = site.getRefPos() - width + 1;
        long right = site.getRefPos() + width - 1;
        byte lastBase = 'N';
        int cnt = 0;
        for (long i = left; i <= right + 1; ++i) {
            byte base = getContext().getReferenceManager().getBase(
                    site.getRefName(), i);
            if (base == 'N' || base != lastBase || i == right + 1) {
                if ((cnt >= longHomopolymerLength && 
                     (i - cnt - longHomopolymerExpansion <= pos &&
                      i - 1 + longHomopolymerExpansion >= pos)) || 
                    (cnt >= shortHomopolymerLength && 
                     (i - cnt - shortHomopolymerExpansion <= pos &&
                      i - 1 + shortHomopolymerExpansion >= pos))) {
                    return false;
                } 
                cnt = 0;
            }
            if (base != 'N') {
                cnt++;
            }
            lastBase = base;
        }
        
        return true;
    }
}
