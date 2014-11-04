package cn.edu.pku.cbi.mosaichunter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReferenceReader {
    
    private final String referenceFile; 
    private BufferedReader reader;       
    private String currentChr = null;
    private int currentChrId = 0;
    private int currentRefPos = 1;
    private int currentLinePos = 0;
    private String currentLine = "";
    private boolean done = false;
    
    public ReferenceReader(String referenceFile) throws IOException {
        this.referenceFile = referenceFile;
        this.reader = new BufferedReader(new FileReader(this.referenceFile));
    }
    
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }
    
    public Entry next() throws IOException {
        while (!done && currentLinePos >= currentLine.length()) {
            nextLine();
        }
        if (done) {
            return null;
        }
        
        Entry ret = new Entry(
                currentChr, 
                currentChrId, 
                currentRefPos, 
                (byte) currentLine.charAt(currentLinePos));
        
        currentLinePos++;
        currentRefPos++;
        return ret;
    }
    
    private void nextLine() throws IOException {
        for (;;) {
            String line = reader.readLine();
            if (line == null) {
                done = true;
                return;
            }
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith(">")) {
                currentChr = line.substring(1);
                if (currentChr.startsWith("chr")) {
                    currentChr = currentChr.substring(3);
                }
                int p = currentChr.indexOf(' ');
                if (p >= 0) {
                    currentChr = currentChr.substring(0, p);
                }
                currentChrId = MosaicHunterHelper.getChrId(currentChr);
                currentRefPos = 1;
                continue;
            }
            currentLine = line;
            currentLinePos = 0;
            break;
        }
    }    
    
    public static class Entry {
        private final String chr;
        private final int chrId;
        private final int position;
        private final byte base;
        
        public Entry(String chr, int chrId, int position, byte base) {
            this.chr = chr;
            this.chrId= chrId;
            this.position = position;
            this.base = base;
        }
        
        public String getChr() {
            return chr;
        }
        
        public int getChrId() {
            return chrId;
        } 
        
        public int getPosition() {
            return position;
        }      
        
        public byte getBase() {
            return base;
        }
    }    
}
