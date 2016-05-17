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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.apache.commons.math3.stat.inference.BinomialTest;

import net.sf.samtools.SAMRecord;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.StatsManager;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.FishersExactTest;

public class CompleteLinkageFilter extends BaseFilter {

    public static final double DEFAULT_BINOM_ERROR_RATE = 1e-3;
    public static final double DEFAULT_BINOM_P_VALUE_CUTOFF = 0.01;
    public static final double DEFAULT_FISHER_P_VALUE_CUTOFF = 0.01;
    
    private final double binomErrorRate;
    private final double binomPValueCutoff;
    private final double fisherPValueCutoff;
    
    public CompleteLinkageFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDouble(
                     name, "binom_error_rate", DEFAULT_BINOM_ERROR_RATE),
             ConfigManager.getInstance().getDouble(
                     name, "binom_p_value_cutoff", DEFAULT_BINOM_P_VALUE_CUTOFF),
             ConfigManager.getInstance().getDouble(
                     name, "fisher_p_value_cutoff", DEFAULT_FISHER_P_VALUE_CUTOFF));
    }
    
    public CompleteLinkageFilter(String name, 
            double binomErrorRate, double binomPValueCutoff, double fisherPValueCutoff) {
        super(name);
        this.binomErrorRate = binomErrorRate;       
        this.binomPValueCutoff = binomPValueCutoff;        
        this.fisherPValueCutoff = fisherPValueCutoff;        
    }   
        
    @Override
    public boolean doFilter(Site site) {  
        
        SAMRecord[] reads = site.getReads();
        if (!doFilter(site, reads)) {
            return false;
        }
        
        SAMRecord[] mates = new SAMRecord[site.getDepth()];
            
        for (int i = 0; i < mates.length; ++i) {
            if (!reads[i].getReadPairedFlag()) {
                continue;
            }
            
            if (mates[i] == null) {
                mates[i] = getContext().getReadsCache().getMate(reads[i]);
            }
            StatsManager.count("mate_query");
            
            // may cause exception for unpaired reads
            if (reads[i].getMateUnmappedFlag()) {
                StatsManager.count("mate_unmapped");
                if (mates[i] != null) {
                    StatsManager.count("mate_unmapped_but_hit");
                }
            } else if (mates[i] == null) {
                StatsManager.count("mate_miss", 1);
                SAMRecord m = null;
                try {
                    m = getContext().getSAMFileReader().queryMate(reads[i]);
                } catch (Exception e) {
                    StatsManager.count("mate_multiple", 1);
                }
                if (m != null && m.getAlignmentStart() != reads[i].getAlignmentStart()) {
                    mates[i] = m;
                    StatsManager.count("mate_miss_true", 1);
                }
                int dis = Math.abs(reads[i].getMateAlignmentStart() - reads[i].getAlignmentStart());
                if (dis > 0) {
                    if (!reads[i].getMateReferenceName().equals(reads[i].getReferenceName())) {
                        StatsManager.count("mate_diff_chr", 1);
                    } else if (dis > 1000000) {
                        StatsManager.count("mate_dis_1000K", 1);
                    } else if (dis > 100000) {
                        StatsManager.count("mate_dis_100K", 1);
                    } else if (dis > 10000) {
                        StatsManager.count("mate_dis_10K", 1);
                    } 
                    
                    if (dis <= 100000) {
                        StatsManager.count("mate_dis", dis);
                    }
                } else {
                    StatsManager.count("mate_dis_zero", 1);
                }
                
            }
        }
        
        boolean result = doFilter(site, mates);
        return result;
    }    
    
    private boolean doFilter(Site site, SAMRecord[] reads) {
        String chrName = site.getRefName();
        int minReadQuality = ConfigManager.getInstance().getInt(null, "min_read_quality", 0);
        int minMappingQuality = ConfigManager.getInstance().getInt(null, "min_mapping_quality", 0);
               

        // find out all related positions
        Map<Integer, PositionEntry> positions = new HashMap<Integer, PositionEntry>();                
        for (int i = 0; i < site.getDepth(); ++i) {
            
            if (reads[i] == null || !chrName.equals(reads[i].getReferenceName())) {
                continue;
            }
            byte base = site.getBases()[i];           
            if (base != site.getMajorAllele() && base != site.getMinorAllele()) {
                continue;
            }
            boolean isMajor = base == site.getMajorAllele();               
            for (int j = 0; j < reads[i].getReadLength(); ++j) {
                if (reads[i].getBaseQualities()[j] < minReadQuality) {
                    continue;
                }
                if (reads[i].getMappingQuality() < minMappingQuality) {
                    continue;
                }
                int id = MosaicHunterHelper.BASE_TO_ID[reads[i].getReadBases()[j]];
                if (id < 0) {
                    continue;
                }             
                int pos = reads[i].getReferencePositionAtReadPosition(j + 1);
                if (pos == site.getRefPos()) {
                    continue;
                }
                PositionEntry entry = positions.get(pos);
                if (entry == null) {
                    entry = new PositionEntry();
                    positions.put(pos, entry);
                }
                entry.count[id]++;
                if (isMajor) {
                    entry.majorCount[id]++;
                } else {
                    entry.minorCount[id]++;
                }                
            }
        }
        
        // for each position
        for (Integer pos : positions.keySet()) {
            PositionEntry entry = positions.get(pos);
            int[] ids = MosaicHunterHelper.sortAlleleCount(entry.count);
            int majorId = ids[0];
            int minorId = ids[1];
            
            int diagonalSum1 = entry.majorCount[majorId] + entry.minorCount[minorId];
            int diagonalSum2 = entry.majorCount[minorId] + entry.minorCount[majorId];
            int totalSum = diagonalSum1 + diagonalSum2;
            int smallDiagonalSum = diagonalSum1;
            if (diagonalSum2 < diagonalSum1) {
                smallDiagonalSum = diagonalSum2;
            }

            if (new BinomialTest().binomialTest(
                    totalSum, smallDiagonalSum, this.binomErrorRate, AlternativeHypothesis.GREATER_THAN)
                    < binomPValueCutoff) {
                continue;
            }
                
            double p = FishersExactTest.twoSided(
                    entry.majorCount[majorId], 
                    entry.majorCount[minorId], 
                    entry.minorCount[majorId],
                    entry.minorCount[minorId]);
            
            
            if (p < fisherPValueCutoff) {
                char major1 = (char) site.getMajorAllele();
                char minor1 = (char) site.getMinorAllele();
                char major2 = (char) MosaicHunterHelper.ID_TO_BASE[majorId];
                char minor2 = (char) MosaicHunterHelper.ID_TO_BASE[minorId];
                site.setMetadata(
                        getName(),
                        new Object[] {
                            pos,
                            "" + major1 + major2 + ":" + entry.majorCount[majorId],
                            "" + major1 + minor2 + ":" + entry.majorCount[minorId],
                            "" + minor1 + major2 + ":" + entry.minorCount[majorId],
                            "" + minor1 + minor2 + ":" + entry.minorCount[minorId],
                            p});
                return false;
            } 
            
        }     
        return true;
    }
    
    private class PositionEntry {
        private int[] majorCount = new int[4];
        private int[] minorCount = new int[4];
        private int[] count = new int[4];
    }
    
}
