package cn.edu.pku.cbi.mosaichunter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.reference.ReferenceManager;

public class MosaicHunterHelper {

    public static final byte[] ID_TO_BASE = new byte[] {'A', 'C', 'G', 'T'};
    public static final int[] BASE_TO_ID = new int[256];
    private static final String chrXName;
    private static final String chrYName;
    
    static {
        for (int i = 0; i < 256; ++i) {
            BASE_TO_ID[i] = -1;
        }
        BASE_TO_ID['A'] = BASE_TO_ID['a'] = 0;
        BASE_TO_ID['C'] = BASE_TO_ID['c'] = 1;
        BASE_TO_ID['G'] = BASE_TO_ID['g'] = 2;
        BASE_TO_ID['T'] = BASE_TO_ID['t'] = 3;
        
        chrXName = ConfigManager.getInstance().get(null, "chr_x_name", "");
        chrYName = ConfigManager.getInstance().get(null, "chr_y_name", "");
        
    }
    
    private MosaicHunterHelper() {
    }
    
    public static boolean isChrX(String chrName) {
        return isChr(chrName, chrXName);
    }
    
    public static boolean isChrY(String chrName) {
        return isChr(chrName, chrYName);
    }
    
    public static boolean isChr(String chrName, String expectedChrName) {
        return expectedChrName != null && 
               expectedChrName.length() > 0 && 
               expectedChrName.equals(chrName);
    }
    
    public static int[] sortAlleleCount(int[] count) {
        int[] ids = new int[count.length];
        for (int i = 0; i < ids.length; ++i) {
            ids[i] = i;
        }
        for (int i = 1; i < ids.length; ++i) {
            for (int j = 0; j < i; ++j) {
                if (count[ids[i]] > count[ids[j]]) {
                    int tmp = ids[i];
                    ids[i] = ids[j];
                    ids[j] = tmp;
                }
            }
        }
        return ids;
    }    
    
    public static List<String[]> readTsvFile(String tsvFile) throws IOException {
        BufferedReader reader = null; 
        List<String[]> ret = new ArrayList<String[]>();
        try {
            reader = new BufferedReader(new FileReader(tsvFile));
            for (;;) {
               String line = reader.readLine();
               if (line == null) {
                   break;
               }
               String[] tokens = line.split("\\t");
               ret.add(tokens);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }  
        return ret;
    }
    
    public static List<Region> readBedFile(
            String bedFile, ReferenceManager referenceManager) throws IOException {
        if (bedFile == null || bedFile.trim().isEmpty()) {
            return new ArrayList<Region>();
        }
        BufferedReader reader = null; 
        List<Region> regions = new ArrayList<Region>();
        
        try {
            reader = new BufferedReader(new FileReader(bedFile));
            for (;;) {
               String line = reader.readLine();
               if (line == null) {
                   break;
               }
               String[] tokens = line.split("\\t");
               if (tokens.length < 3) {
                   continue;
               }
               String chrName = tokens[0];
               int chrId = referenceManager.getReferenceId(chrName);
               if (chrId >= 0) {
                   Region region = new Region(
                       chrName,
                       chrId,
                       Integer.parseInt(tokens[1]) + 1,
                       Integer.parseInt(tokens[2]));
                   regions.add(region);
               }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }  
        return regions;
    }
    
    public static List<Region> generateRandomRegions(
            Random random, int n, int length, ReferenceManager referenceManager) {
        
        int chrN = referenceManager.getReferenceNumber();
        long sum = 0;
        long[] sizes = new long[chrN];
        for (int i = 0; i < chrN; ++i) {
            sizes[i] = referenceManager.getReferenceLength(i) - length + 1;
            if (sizes[i] < 0) {
                sizes[i] = 0;
            }
            sum += referenceManager.getReferenceLength(i);
        }
        List<Region> ret = new ArrayList<Region>();
        for (int i = 0; i < n; ++i) {
            Region r = null;
            long p = Math.abs(random.nextLong()) % sum;
            for (int j = 0; j < chrN; ++j) {
                if (p < sizes[j]) {
                    r = new Region(
                            referenceManager.getReference(j).getName(), 
                            j, 
                            (int) p, 
                            (int) (p + length - 1));
                    break;
                } else {
                    p -= sizes[j];
                }
            }
            if (r != null) {
                ret.add(r);
            }
        }
        return ret;
    }
    
    public static List<Region> sortAndCombineRegions(List<Region> regions) {
        Collections.sort(regions);
        List<Region> ret = new ArrayList<Region>();
        Region current = null;
        for (Region region : regions) {
            if (region.getStart() < 0 || region.getEnd() < region.getStart()) {
                continue;
            }
            if (current == null) {
                current = region;
                continue;
            } 
            if (current.getChrId() == region.getChrId() && 
                current.getEnd() >= region.getStart()) {
                current = new Region(
                        current.getChr(), 
                        current.getChrId(), 
                        current.getStart(), 
                        Math.max(current.getEnd(), region.getEnd()));
            } else {
                ret.add(current);
                current = region;
            }
        }
        if (current != null) {
            ret.add(current);
        }
        return ret;
    }
    

}
