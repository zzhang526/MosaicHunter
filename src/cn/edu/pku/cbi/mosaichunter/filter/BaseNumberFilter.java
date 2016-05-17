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

public class BaseNumberFilter extends BaseFilter {

    public static final int DEFAULT_MIN_MINOR_ALLELE_NUMBER = 5;
    public static final double DEFAULT_MIN_MINOR_ALLELE_PERCENTAGE = 5;
    public static final double DEFAULT_MAX_MINOR_ALLELE_PERCENTAGE = 100;
    
    private final int minMinorAlleleNumber;
    private final double minMinorAllelePercentage;
    private final double maxMinorAllelePercentage;
    
    public BaseNumberFilter(String name) {
        this(name,
             ConfigManager.getInstance().getInt(
                     name, "min_minor_allele_number", DEFAULT_MIN_MINOR_ALLELE_NUMBER),
             ConfigManager.getInstance().getDouble(
                     name, "min_minor_allele_percentage", DEFAULT_MIN_MINOR_ALLELE_PERCENTAGE),
             ConfigManager.getInstance().getDouble(
                     name, "max_minor_allele_percentage", DEFAULT_MAX_MINOR_ALLELE_PERCENTAGE));
    }
    
    public BaseNumberFilter(String name, int minMinorAlleleNumber, 
            double minMinorAllelePercentage, double maxMinorAllelePercentage) {
        super(name);
        this.minMinorAlleleNumber = minMinorAlleleNumber;
        this.minMinorAllelePercentage = minMinorAllelePercentage / 100.0;
        this.maxMinorAllelePercentage = maxMinorAllelePercentage / 100.0;
    }
    
    @Override
    public boolean doFilter(Site site) {
        return site.getMinorAlleleCount() >= minMinorAlleleNumber &&
               site.getMinorAlleleCount() >= 
                       site.getDepth() * minMinorAllelePercentage &&
               site.getMinorAlleleCount() <
                       site.getDepth() * maxMinorAllelePercentage;
    }
    
}
