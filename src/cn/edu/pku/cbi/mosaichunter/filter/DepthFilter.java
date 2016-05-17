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

public class DepthFilter extends BaseFilter {

    public static final int DEFAULT_MIN_DEPTH = 25;
    public static final int DEFAULT_MAX_DEPTH = 150;
    
    private final int minDepth;
    private final int maxDepth;
    
    public DepthFilter(String name) {
        this(name,
             ConfigManager.getInstance().getInt(name, "min_depth", DEFAULT_MIN_DEPTH),
             ConfigManager.getInstance().getInt(name, "max_depth", DEFAULT_MAX_DEPTH));
    }
    
    public DepthFilter(String name, int minDepth, int maxDepth) {
        super(name);
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
    }
    
    @Override
    public boolean doFilter(Site site) {  
        return site.getDepth() >= minDepth && site.getDepth() <= maxDepth;
    }
    
}
