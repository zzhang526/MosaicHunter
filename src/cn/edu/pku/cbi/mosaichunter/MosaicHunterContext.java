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

import cn.edu.pku.cbi.mosaichunter.reference.ReferenceManager;
import net.sf.samtools.SAMFileReader;

public class MosaicHunterContext {

    private final SAMFileReader samFileReader;
    private final ReferenceManager referenceManager;
    private final ReadsCache readsCache;

    
    public MosaicHunterContext(
            SAMFileReader samFileReader, 
            ReferenceManager referenceManager, 
            ReadsCache readsCache) {
        this.samFileReader = samFileReader;
        this.referenceManager = referenceManager;
        this.readsCache = readsCache;
    }
    
    public SAMFileReader getSAMFileReader() {
        return samFileReader;
    }
    
    public ReferenceManager getReferenceManager() {
        return referenceManager;
    }
    
    public ReadsCache getReadsCache() {
        return readsCache;
    }
    
}
