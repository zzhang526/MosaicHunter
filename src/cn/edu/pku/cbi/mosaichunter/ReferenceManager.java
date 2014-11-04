package cn.edu.pku.cbi.mosaichunter;

import java.util.HashMap;
import java.util.Map;

public class ReferenceManager {

    public String[] DEFAULT_CHROMOSOME_ORDER = new String[] {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", 
            "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y"  
    };
    
    private final String[] chromosomeOrder;
    private final int[] chromosomeSizes;
    private final Map<String, Integer> chrIds = new HashMap<String, Integer>();
    
    
    private ReferenceManager(String[] chromosomeOrder, int[] chromosomeSizes) {
        this.chromosomeOrder = chromosomeOrder;
        this.chromosomeSizes = chromosomeSizes;
        for (int i = 0; i < chromosomeOrder.length; ++i) {
            chrIds.put(chromosomeOrder[i], i + 1);
        }
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
}
