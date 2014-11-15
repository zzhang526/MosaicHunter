package cn.edu.pku.cbi.mosaichunter.filter;

import java.util.HashMap;
import java.util.Map;

import net.sf.samtools.SAMRecord;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.StatsManager;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.FishersExactTest;

public class CompleteLinkageFilter extends BaseFilter {

    public static final double DEFAULT_MIN_P_VALUE = 0.01;
    
    private final double minPValue;
    
    public CompleteLinkageFilter(String name) {
        this(name,
             ConfigManager.getInstance().getDouble(name, "min_p_value", DEFAULT_MIN_P_VALUE));
    }
    
    public CompleteLinkageFilter(String name, double minPValue) {
        super(name);
        this.minPValue = minPValue;        
    }   
        
    @Override
    public boolean doFilter(FilterEntry filterEntry) {  
        
        SAMRecord[] reads = filterEntry.getReads();
        if (!doFilter(filterEntry, reads)) {
            return false;
        }
        
        SAMRecord[] mates = filterEntry.getMates();
        if (mates == null) {
            mates = new SAMRecord[filterEntry.getDepth()];
            filterEntry.setMates(mates);
        }
        for (int i = 0; i < mates.length; ++i) {
            if (mates[i] == null) {
                mates[i] = filterEntry.getReadCache().getMate(reads[i]);
            }
            StatsManager.count("mate_query");
            if (reads[i].getMateUnmappedFlag()) {
                StatsManager.count("mate_unmapped");
                if (mates[i] != null) {
                    StatsManager.count("mate_unmapped_but_hit");
                }
            } else if (mates[i] == null) {
                StatsManager.count("mate_miss", 1);
                SAMRecord m = filterEntry.getSAMFileReader().queryMate(reads[i]);
                if (m != null && m.getAlignmentStart() != reads[i].getAlignmentStart()) {
                    mates[i] = m;
                    StatsManager.count("mate_miss_true", 1);
                    /*
                    System.out.println(filterEntry.getChrName() + " " + filterEntry.getRefPos());
                    System.out.println(reads[i].getMateAlignmentStart() + " " + reads[i].getAlignmentStart());
                    System.out.println(mates[i].getMateAlignmentStart() + " " + mates[i].getAlignmentStart());
                    */
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
        
        boolean result = doFilter(filterEntry, mates);
        return result;
    }    
    
    private boolean doFilter(FilterEntry filterEntry, SAMRecord[] reads) {
        String chrName = filterEntry.getChrName();
        int minReadQuality = ConfigManager.getInstance().getInt(null, "min_read_quality", 0);
        int minMappingQuality = ConfigManager.getInstance().getInt(null, "min_mapping_quality", 0);
               

        // find out all related positions
        Map<Integer, PositionEntry> positions = new HashMap<Integer, PositionEntry>();                
        for (int i = 0; i < filterEntry.getDepth(); ++i) {
            
            if (reads[i] == null || !chrName.equals(MosaicHunterHelper.getChr(reads[i].getReferenceName()))) {
                continue;
            }
            byte base = filterEntry.getBases()[i];           
            if (base != filterEntry.getMajorAllele() && base != filterEntry.getMinorAllele()) {
                continue;
            }
            boolean isMajor = base == filterEntry.getMajorAllele();               
            for (int j = 0; j < reads[i].getReadLength(); ++j) {
                if (reads[i].getBaseQualities()[j] < minReadQuality) {
                    continue;
                }
                if (reads[i].getMappingQuality() < minMappingQuality) {
                    continue;
                }
                int id = MosaicHunterHelper.getBaseId(reads[i].getReadBases()[j]);
                if (id < 0) {
                    continue;
                }             
                int pos = reads[i].getReferencePositionAtReadPosition(j + 1);
                if (pos == filterEntry.getRefPos()) {
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
            if (entry.majorCount[majorId] + entry.minorCount[minorId] > 1 &&
                entry.majorCount[minorId] + entry.minorCount[majorId] > 1) {
                continue;
            }
            
            double p = FishersExactTest.twoSided(
                    entry.majorCount[majorId],
                    entry.majorCount[minorId],
                    entry.minorCount[majorId],
                    entry.minorCount[minorId]);  
            if (p < minPValue) {
                char major1 = (char) filterEntry.getMajorAllele();
                char minor1 = (char) filterEntry.getMinorAllele();
                char major2 = MosaicHunterHelper.idToBase(majorId);
                char minor2 = MosaicHunterHelper.idToBase(minorId);
                filterEntry.setMetadata(
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
