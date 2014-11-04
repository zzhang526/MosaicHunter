package cn.edu.pku.cbi.mosaichunter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.filter.Filter;
import cn.edu.pku.cbi.mosaichunter.filter.FilterEntry;
import cn.edu.pku.cbi.mosaichunter.filter.FilterFactory;
import net.sf.samtools.AlignmentBlock;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;


public class BamScanner {
    
    private final String inputFile;
    private final String indexFile;    
    private final String referenceFile;    
    private final int maxDepth;
    private final Filter filter;
    private final Filter postProcessfilter;  
    private final long seed;
    private final boolean depthSampling;
    private final Random random;
    
    public BamScanner() throws Exception {
        this(ConfigManager.getInstance().get(null, "input_file", null),
             ConfigManager.getInstance().get(null, "index_file", null),
             ConfigManager.getInstance().get(null, "reference_file", null),
             FilterFactory.create(ConfigManager.getInstance().get(null, "in_process_filter_name", null)),
             FilterFactory.create(ConfigManager.getInstance().get(null, "post_process_filter_name", null)),
             ConfigManager.getInstance().getInt(null, "max_depth"),
             ConfigManager.getInstance().getLong(null, "seed", System.currentTimeMillis()),
             ConfigManager.getInstance().getBoolean(null, "depth_sampling", false)
             );        
    }
    
    public BamScanner(String inputFile, String indexFile, String referenceFile, 
            Filter inProcessFilter, Filter postProcessfilter, int maxDepth, long seed, boolean depthSampling) throws Exception {
        this.inputFile = inputFile;
        this.indexFile = indexFile;
        this.referenceFile = referenceFile;
        this.filter = inProcessFilter;
        this.postProcessfilter = postProcessfilter;
        this.maxDepth = maxDepth;
        this.seed = seed;
        this.depthSampling = depthSampling;
        this.random = new Random(this.seed);
        
        inProcessFilter.init();
        postProcessfilter.init();
    }
    
    public void scan() throws Exception  {       
        
        BufferedReader referenceReader = new BufferedReader(new FileReader(referenceFile));
        ReferenceScanner referenceScanner = new ReferenceScanner(referenceReader);
        
        SAMFileReader input = new SAMFileReader(
                new File(inputFile), 
                indexFile == null ? null : new File(indexFile));
     
        input.setValidationStringency(ValidationStringency.SILENT);
        
        SAMFileHeader h = input.getFileHeader();
        int chrN = 24;
        long refSize = 0;
        long[] chrSizes = new long[chrN];
        String[] chrNames = new String[chrN];
        for (int i = 0; i < chrN; ++i) {
            chrSizes[i] = h.getSequenceDictionary().getSequence(i).getSequenceLength();
            chrNames[i] = h.getSequenceDictionary().getSequence(i).getSequenceName();
            refSize += chrSizes[i];
        }
        
        long processedEntries = 0;
        long processedSites = 0;
        long totalSites = 0;
        long startTime = System.currentTimeMillis();
        
        ConfigManager config = ConfigManager.getInstance();
        int minReadQuality = config.getInt(null, "min_read_quality", 0);
        int minMappingQuality = config.getInt(null, "min_mapping_quality", 0);
        ReadCache readCache = new ReadCache(ConfigManager.getInstance().getInt(
                null, "max_recent_reads" , 100000));
        
        String chr = config.get(null, "chr", null);
        String inputBedFile = config.get(null, "input_bed_file", null);
        boolean inputSampling = config.getBoolean(null, "input_sampling", false);
        
        List<Region> regions = null;
        if (chr != null && !chr.trim().isEmpty()) {
            int chrId = MosaicHunterHelper.getChrId(chr);
            if (chrId <= 0) {
                input.close();
                throw new Exception("invalid chr parameter");
            }
            int startPosition = config.getInt(null, "start_position", 1);
            int endPosition = config.getInt(null, "end_position", Integer.MAX_VALUE);
            if (endPosition > chrSizes[chrId - 1]) {
                endPosition = (int) chrSizes[chrId - 1];
            }
            regions = new ArrayList<Region>(Collections.singletonList(
                    new Region(chr, chrId, startPosition, endPosition)));
        } else if (inputBedFile != null && !inputBedFile.trim().isEmpty()) {
            regions = MosaicHunterHelper.readBedFile(inputBedFile);
        } else if (inputSampling) {
            int inputSamplingRegions = config.getInt(null, "input_sampling_regions", 1);
            int inputSamplingSize  = config.getInt(null, "input_sampling_size", 1);
            regions = MosaicHunterHelper.generateRandomRegions(
                    random, chrSizes, chrNames, inputSamplingRegions, inputSamplingSize);
        }
        if (regions != null) {
            regions = MosaicHunterHelper.sortAndCombineRegions(regions);
            for (Region r : regions) {
                totalSites += r.getEnd() - r.getStart() + 1;
            }
        } else {
            regions = Collections.singletonList(null);
            totalSites = refSize;
        }
        
        
        List<FilterEntry> filterEntries = new ArrayList<FilterEntry>();
        
        StatsManager.start("in_process");
        
        System.out.println(
                new Date() + " -" +
                " Time(s):" + (System.currentTimeMillis() - startTime) / 1000 + 
                " Reads:" + processedEntries +
                " Sites:" + 0 + "/" + totalSites + 
                " Progress:" + String.format("%.2f", 0.0) + "%");
        
        for (Region region : regions) {
            int startPosition = 1;
            int endPosition = Integer.MAX_VALUE;
            SAMRecordIterator it = null;
            if (region == null) {
                it = input.iterator();
       
            } else {
                it = input.queryOverlapping(region.getChr(), region.getStart(), region.getEnd()); 
                startPosition = region.getStart();
                endPosition = region.getEnd();   
                //System.out.println(region.getChr() + " " + startPosition + " " + endPosition + " " + processedEntries);
            }
            
            String lastRefName = null;
            long lastRefPos = startPosition;
            
            LinkedList<ScanEntry> scanEntries = new LinkedList<ScanEntry>();
            SAMRecordIteratorBuffer itBuffer = new SAMRecordIteratorBuffer(
                    it, ConfigManager.getInstance().getInt(null, "read_buffer_size" , 100000), readCache);
            
            while (true) {
                ScanEntry currentEntry = null;
                while (itBuffer.hasNext() && currentEntry == null) {
                    SAMRecord next = itBuffer.next(); 
                    processedEntries++;
                    if (processedEntries % 1000000 == 0) {
                        long done = processedSites + lastRefPos - startPosition;
                        double progress = (double) done * 100 / totalSites;
                        System.out.println(
                                new Date() + " -" +
                                " Time(s):" + (System.currentTimeMillis() - startTime) / 1000 + 
                                " Reads:" + processedEntries +
                                " Sites:" + done + "/" + totalSites + 
                                " Progress:" + String.format("%.2f", progress) + "%");
                    }
                    if (next.getDuplicateReadFlag()) {
                        continue;
                    }
                    if (next.getMappingQuality() < minMappingQuality) {
                        continue;
                    }
                    if (MosaicHunterHelper.getChrId(MosaicHunterHelper.getChr(next.getReferenceName())) <= 0) {
                        continue;
                    }
                    currentEntry = new ScanEntry();
                    currentEntry.record = next;
                    currentEntry.chr = MosaicHunterHelper.getChr(next.getReferenceName());
                    currentEntry.alignmentBlocks = new LinkedList<AlignmentBlock>(
                            currentEntry.record.getAlignmentBlocks());
                }
                long currentRefPos = Long.MAX_VALUE;
                if (currentEntry != null && currentEntry.chr.equals(lastRefName)) {
                    currentRefPos = currentEntry.record.getAlignmentStart();
                } 
                for (long pos = lastRefPos; pos < currentRefPos && !scanEntries.isEmpty(); ++pos) {
                    int depth = 0;
                    int cnt = 0;
                    int currentDepth = 150;
                    SAMRecord[] baseRecords = new SAMRecord[currentDepth];
                    int[] basePos = new int[currentDepth];
                    byte[] bases = new byte[currentDepth];
                    byte[] baseQualities = new byte[currentDepth];
                    
                    for (Iterator<ScanEntry> it2 = scanEntries.iterator(); it2.hasNext();) {                                              
                        ScanEntry entry = it2.next();
                        AlignmentBlock block = null;
                        while (!entry.alignmentBlocks.isEmpty()) {
                            block = entry.alignmentBlocks.getFirst();
                            if (pos < block.getReferenceStart() + block.getLength()) {
                                break;
                            }
                            entry.alignmentBlocks.pop();
                            block = null;
                        }
                        if (block == null) {
                            it2.remove();
                        } else if (pos >= block.getReferenceStart()) {
                            int p = (int) (pos - block.getReferenceStart() + block.getReadStart() - 1);
                            byte quality = entry.record.getBaseQualities()[p];
                            if (quality >= minReadQuality) {
                                cnt++;
                                if (depth >= currentDepth) {
                                    currentDepth *= 2;
                                    if (currentDepth > maxDepth + 1) {
                                        currentDepth = maxDepth + 1;
                                    }
                                    SAMRecord[] newBaseRecords = new SAMRecord[currentDepth];
                                    int[] newBasePos = new int[currentDepth];
                                    byte[] newBases = new byte[currentDepth];
                                    byte[] newBaseQualities = new byte[currentDepth];
                                    System.arraycopy(baseRecords, 0, newBaseRecords, 0, depth);
                                    System.arraycopy(basePos, 0, newBasePos, 0, depth);
                                    System.arraycopy(bases, 0, newBases, 0, depth);
                                    System.arraycopy(baseQualities, 0, newBaseQualities, 0, depth);
                                    baseRecords = newBaseRecords;
                                    basePos = newBasePos;
                                    bases = newBases;
                                    baseQualities = newBaseQualities;
                                    StatsManager.count("depth_array_expand");
                                }
                                int ii = -1;
                                if (depth < maxDepth) {
                                    ii = depth;
                                    depth++;
                                } else if (depthSampling && random.nextInt(cnt) < maxDepth) {
                                    ii = random.nextInt(maxDepth);
                                }
                                if (ii >= 0) {
                                    baseRecords[ii] = entry.record;
                                    basePos[ii] = p;
                                    bases[ii] = entry.record.getReadBases()[p];
                                    baseQualities[ii] = quality;
                                }
                            }
                        }
                    }  
                    
                    if (depth > 0 && pos >= startPosition && pos <= endPosition) {
                        byte refBase = referenceScanner.getBaseAt(lastRefName, pos);
                        if (refBase != 0) {
                            refBase = (byte) Character.toUpperCase(refBase);
                            FilterEntry filterEntry = new FilterEntry(
                                    input,
                                    readCache,
                                    lastRefName,
                                    pos,
                                    refBase,
                                    depth,
                                    bases,
                                    baseQualities,
                                    baseRecords,
                                    basePos);
                            
                            if (filter.filter(filterEntry)) {
                                // TODO limit size?
                                filterEntries.add(filterEntry);                            
                            }
                        }
                    }                     
                }
                
                if (currentEntry == null) {
                    break;
                }
                if (currentEntry.chr.equals(lastRefName)) {
                    lastRefPos = currentRefPos;
                } else {
                    lastRefName = currentEntry.chr;
                    lastRefPos = currentEntry.record.getAlignmentStart();
                }            
                scanEntries.add(currentEntry);
            }  
            processedSites += endPosition - startPosition + 1;
            it.close();
        }
        System.out.println(
                new Date() + " -" +
                " Time(s):" + (System.currentTimeMillis() - startTime) / 1000 + 
                " Reads:" + processedEntries +
                " Sites:" + totalSites + "/" + totalSites + 
                " Progress:" + String.format("%.2f", 100.0) + "%");
        
        StatsManager.end("in_process");
        
        StatsManager.start("post_process");
        filterEntries = postProcessfilter.filter(filterEntries);
        StatsManager.end("post_process");
        
        filter.printStats(true);
        postProcessfilter.printStats(false);
        
        input.close();
        referenceReader.close();
    }

    public void close() throws Exception {
        filter.close();
        postProcessfilter.close();
    }
    
   
    private static class ScanEntry {
        private String chr;
        private SAMRecord record;
        private LinkedList<AlignmentBlock> alignmentBlocks;
    }
    
    
    private class SAMRecordIteratorBuffer {
        
        private final int bufferSize;
        private final SAMRecord[] buffer;
        private final SAMRecordIterator it;
        private final ReadCache readCache;
        
        private int start = 0;
        private int end = 0;
        private boolean hasNext;
        
        public SAMRecordIteratorBuffer(SAMRecordIterator it, int bufferSize, ReadCache readCache) {
            this.it = it;
            this.readCache = readCache;
            this.bufferSize = bufferSize;
            buffer = new SAMRecord[bufferSize];
            
            while(it.hasNext() && end < bufferSize) {
                buffer[end] = it.next();
                readCache.cacheRead(buffer[end]);
                end++;
            }
            hasNext = it.hasNext();
        }
        
        public SAMRecord next() {
            SAMRecord r = null;
            if (hasNext) {
                r = buffer[start];
                buffer[start] = it.next();
                readCache.cacheRead(buffer[start]);
                hasNext = it.hasNext();
            } else if (end == bufferSize) {
                end = start;
                r = buffer[start];
            } else if (end == start) {
                return null;
            } else {
                r = buffer[start];
            }
            start++;
            if (start >= bufferSize) {
                start = 0;
            }
            return r;
        }
        
        public boolean hasNext() {
            return hasNext || start != end;
        }
    }
    
    private class ReferenceScanner {
        private final BufferedReader reader;
        private String currentName = null;
        private int currentChrId = 0;
        private long currentPos = 1;
        private String currentLine = "";
        private boolean done = false;
        
        public ReferenceScanner(BufferedReader reader) {
            this.reader = reader;
        }
        
        public byte getBaseAt(String chrName, long pos) throws IOException {
            int chrId = MosaicHunterHelper.getChrId(chrName);
            while (currentChrId < chrId && !done) {
                nextLine();
            }
            if (done || currentChrId != chrId || pos < currentPos) {
                return 0;
            }
            
            while (pos >= currentPos + currentLine.length() && 
                   currentChrId == chrId &&
                   !done) {
                nextLine();
            }
            if (done || currentChrId != chrId) {
                return 0;
            }            
            return (byte) currentLine.charAt((int) (pos - currentPos));
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
                    currentName = line.substring(1).trim();
                    int p = currentName.indexOf(' ');
                    if (p >= 0) {
                        currentName = currentName.substring(0, p);
                    }
                    if (currentName.startsWith("chr")) {
                        currentName = currentName.substring(3);
                    }
                    currentChrId = MosaicHunterHelper.getChrId(currentName);
                    currentPos = 1;
                    currentLine = "";
                    continue;
                }
                currentPos += currentLine.length();
                currentLine = line;
                break;
            }
        }
    }
}

