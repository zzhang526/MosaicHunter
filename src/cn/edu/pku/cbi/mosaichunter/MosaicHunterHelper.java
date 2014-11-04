package cn.edu.pku.cbi.mosaichunter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MosaicHunterHelper {

    private MosaicHunterHelper() {
    }
    
    public static String getChr(String name) {
        if (name.startsWith("chr")) {
            name = name.substring(3);
        }
        return name;
    }
    
    public static int getChrId(String name) {
        if (name.equals("X")) {
            return 23;                
        } else if (name.equals("Y")) {
            return 24;                
        } else {
            try {
                return Integer.parseInt(name);
            } catch (Exception e) {                    
                return 0;
            }
        }
    } 
    
    public static int getBaseId(byte base) {
        return getBaseId((char) base);
    }
    
    public static int getBaseId(char base) {
        return "ACGT".indexOf(Character.toUpperCase(base));
    }
    
    public static char idToBase(int id) {
        return "ACGT".charAt(id);
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
    
    public static List<Region> readBedFile(String bedFile) throws IOException {
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
               Region region = new Region(
                       chrName,
                       getChrId(chrName),
                       Integer.parseInt(tokens[1]) + 1,
                       Integer.parseInt(tokens[2]));
               regions.add(region);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }  
        return regions;
    }
    
    public static List<Region> generateRandomRegions(
            Random random, long[] chrSizes, String[] chrNames, int n, int length) {
        int chrN = chrSizes.length;
        long sum = 0;
        long[] sizes = new long[chrSizes.length];
        for (int i = 0; i < chrN; ++i) {
            sizes[i] = chrSizes[i] - length + 1;
            if (sizes[i] < 0) {
                sizes[i] = 0;
            }
            sum += chrSizes[i];
        }
        List<Region> ret = new ArrayList<Region>();
        for (int i = 0; i < n; ++i) {
            Region r = null;
            long p = Math.abs(random.nextLong()) % sum;
            for (int j = 0; j < chrN; ++j) {
                if (p < sizes[j]) {
                    r = new Region(chrNames[j], j + 1, (int) p, (int) (p + length - 1));
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
