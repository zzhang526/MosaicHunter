package cn.edu.pku.cbi.mosaichunter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private final int maxSites;
    private final Filter inProcessFilter;
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
             ConfigManager.getInstance().getInt(null, "max_sites", 10000),
             ConfigManager.getInstance().getLong(null, "seed", System.currentTimeMillis()),
             ConfigManager.getInstance().getBoolean(null, "depth_sampling", false)
             );        
    }
    
    public BamScanner(String inputFile, String indexFile, String referenceFile, 
            Filter inProcessFilter, Filter postProcessfilter, int maxDepth, int maxSites, long seed, boolean depthSampling) throws Exception {
        this.inputFile = inputFile;
        this.indexFile = indexFile;
        this.referenceFile = referenceFile;
        this.inProcessFilter = inProcessFilter;
        this.postProcessfilter = postProcessfilter;
        this.maxDepth = maxDepth;
        this.maxSites = maxSites;
        this.seed = seed;
        this.depthSampling = depthSampling;
        this.random = new Random(this.seed);
        
       
    }
    
    public void scan() throws Exception  {       
        
        System.out.println(new Date() + " Initializing...");
        
        ArrayManager arrayManager = new ArrayManager(160, Math.max(160, maxDepth + 1));
     
        SAMFileReader samFileReader = new SAMFileReader(
                new File(inputFile), 
                indexFile == null ? null : new File(indexFile));
        samFileReader.setValidationStringency(ValidationStringency.SILENT);
     
        ReferenceManager referenceManager = null;
        if (ConfigManager.getInstance().getBoolean(null, "enable_reference_cache", false)) {
            String od = ConfigManager.getInstance().get(null, "output_dir", ".");
            File cacheFile = new File(od, new File(referenceFile + ".cache").getName());
            System.out.println(new Date() + " Reading reference from cache file: " + cacheFile.getAbsolutePath());
            if (cacheFile.isFile()) {
                ObjectInputStream ois = null;
                try {
                    FileInputStream fis = new FileInputStream(cacheFile);
                    ois = new ObjectInputStream(fis);
                    referenceManager = (ReferenceManager) ois.readObject();
                    ois.close();
                } catch (Exception e) {
                    System.out.println(new Date() + " Cannot read cache file: " + cacheFile.getAbsolutePath() + " " + e.getMessage());
                } finally {
                    if (ois != null) {
                        try {
                            ois.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
            
            if (referenceManager == null) {
                System.out.println(new Date() + " Reading reference from file: " + referenceFile);
                referenceManager = new ReferenceManager(referenceFile);
                System.out.println(new Date() + " Writing reference to cache file: " + cacheFile.getAbsolutePath());
                ObjectOutputStream oos = null;
                try {
                    FileOutputStream fos = new FileOutputStream(cacheFile);
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(referenceManager);
                    oos.close(); 
                } catch (Exception e) {
                    System.out.println(new Date() + " Cannot write cache file: " + cacheFile.getAbsolutePath() + " " + e.getMessage());
                    e.printStackTrace();
                    
                } finally {
                    if (oos != null) {
                        try {
                            oos.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        } else {
            System.out.println(new Date() + " Reading reference from file: " + referenceFile);
            referenceManager = new ReferenceManager(referenceFile);
        }
        
        
        System.out.println(new Date() + " Initiazing filters...");
        inProcessFilter.init();
        postProcessfilter.init();
        
        
        // scan
        
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
        long depthSum = 0;
        long depthCount = 0;
        
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
                samFileReader.close();
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
        
        System.out.println(new Date() + " Scanning...");

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
                                " Progress:" + String.format("%.2f", progress) + "%" + 
                                " " + lastRefName + ":" + lastRefPos);
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
                    int currentDepth = (int)(depthSum / (depthCount + 1) * 2);
                    currentDepth = Math.max(currentDepth, 100);
                    currentDepth = Math.min(currentDepth, maxDepth + 1);
                    
                    SAMRecord[] reads = arrayManager.getSAMRecordArray(currentDepth);
                    short[] basePos = arrayManager.getShortArray(currentDepth);
                    byte[] bases = arrayManager.getByteArray(currentDepth);
                    byte[] baseQualities = arrayManager.getByteArray(currentDepth);
                    currentDepth = baseQualities.length;
                    
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
                            short p = (short) (pos - block.getReferenceStart() + block.getReadStart() - 1);
                            byte quality = entry.record.getBaseQualities()[p];
                            if (quality >= minReadQuality) {
                                cnt++;
                                if (depth >= currentDepth) {
                                    SAMRecord[] newReads = arrayManager.getSAMRecordArray(currentDepth + 1);
                                    short[] newBasePos = arrayManager.getShortArray(currentDepth + 1);
                                    byte[] newBases = arrayManager.getByteArray(currentDepth + 1);
                                    byte[] newBaseQualities = arrayManager.getByteArray(currentDepth + 1);
                                    currentDepth = newBaseQualities.length;
                                    
                                    System.arraycopy(reads, 0, newReads, 0, depth);
                                    System.arraycopy(basePos, 0, newBasePos, 0, depth);
                                    System.arraycopy(bases, 0, newBases, 0, depth);
                                    System.arraycopy(baseQualities, 0, newBaseQualities, 0, depth);
                                    
                                    arrayManager.returnSAMRecordArray(reads);
                                    arrayManager.returnShortArray(basePos);
                                    arrayManager.returnByteArray(bases);
                                    arrayManager.returnByteArray(baseQualities);
                                    
                                    reads = newReads;
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
                                    reads[ii] = entry.record;
                                    basePos[ii] = p;
                                    bases[ii] = entry.record.getReadBases()[p];
                                    baseQualities[ii] = quality;
                                }
                            }
                        }
                    }  
                    
                    depthSum += depth;
                    depthCount++;
                    
                    if (depth > 0 && pos >= startPosition && pos <= endPosition) {
                        //byte refBase = referenceScanner.getBaseAt(lastRefName, pos);
                        byte refBase = referenceManager.getBase(lastRefName, (int) pos);
                        
                        if (refBase != 0 && refBase != 'N') {
                            refBase = (byte) Character.toUpperCase(refBase);
                            FilterEntry filterEntry = new FilterEntry(
                                    samFileReader,
                                    readCache,
                                    referenceManager,
                                    lastRefName,
                                    pos,
                                    refBase,
                                    depth,
                                    bases,
                                    baseQualities,
                                    reads,
                                    null,
                                    basePos);
                            
                            if (filterEntries.size() < maxSites && inProcessFilter.filter(filterEntry)) {
                                filterEntries.add(filterEntry);   
                                reads = null;
                                basePos = null;
                                bases = null;
                                baseQualities = null;
                            }
                        }
                    }                    
                    arrayManager.returnSAMRecordArray(reads);
                    arrayManager.returnShortArray(basePos);
                    arrayManager.returnByteArray(bases);
                    arrayManager.returnByteArray(baseQualities);
                    
                }
                
                if (currentEntry == null) {
                    break;
                }
                if (currentEntry.chr.equals(lastRefName)) {
                    lastRefPos = currentRefPos;
                } else {
                    if (region == null) {
                        // TODO: should be chr length
                        processedSites += lastRefPos + 1;
                    }
                    lastRefName = currentEntry.chr;
                    lastRefPos = currentEntry.record.getAlignmentStart();
                }            
                scanEntries.add(currentEntry);
            }  
            if (region != null) {
                processedSites += endPosition - startPosition + 1;
            }
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
        postProcessfilter.filter(filterEntries);
        StatsManager.end("post_process");
        
        for (FilterEntry filterEntry : filterEntries) {
            arrayManager.returnSAMRecordArray(filterEntry.getReads());
            arrayManager.returnShortArray(filterEntry.getBasePos());
            arrayManager.returnByteArray(filterEntry.getBases());
            arrayManager.returnByteArray(filterEntry.getBaseQualities());
        }
        
        inProcessFilter.printStats(true);
        postProcessfilter.printStats(false);
        
        input.close();
        samFileReader.close();
        inProcessFilter.close();
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
    
}

