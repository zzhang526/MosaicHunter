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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.config.Validator;
import cn.edu.pku.cbi.mosaichunter.filter.Filter;
import cn.edu.pku.cbi.mosaichunter.filter.FilterFactory;
import cn.edu.pku.cbi.mosaichunter.reference.Reference;
import cn.edu.pku.cbi.mosaichunter.reference.ReferenceManager;
import net.sf.samtools.AlignmentBlock;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;


public class BamScanner {
        
    public static final String[] DEFAULT_VALID_REFERENCES = new String[] {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", 
            "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y"  
    };
    
    private final String inputFile;
    private final String indexFile;    
    private final String referenceFile;    
    private final int maxDepth;
    private final int maxSites;
    private final boolean removeDuplicates;
    private final int removeFlags;
    private final Filter inProcessFilter;
    private final Filter postProcessFilter;  
    private final long seed;
    private final boolean depthSampling;
    private final Random random;
    
    public BamScanner() throws Exception {
        this(ConfigManager.getInstance().get(null, "input_file", null),
             ConfigManager.getInstance().get(null, "index_file", null),
             ConfigManager.getInstance().get(null, "reference_file", null),
             FilterFactory.create(ConfigManager.getInstance().get(
                     null, "in_process_filter_name", null)),
             FilterFactory.create(ConfigManager.getInstance().get(
                     null, "post_process_filter_name", null)),
             ConfigManager.getInstance().getInt(null, "max_depth"),
             ConfigManager.getInstance().getInt(null, "max_sites", 500000),
             ConfigManager.getInstance().getBoolean(null, "remove_duplicates", true),
             ConfigManager.getInstance().getIntFlags(null, "remove_flags", 0),
             ConfigManager.getInstance().getLong(null, "seed", System.currentTimeMillis()),
             ConfigManager.getInstance().getBoolean(null, "depth_sampling", false)
             );        
    }
    
    public BamScanner(String inputFile, String indexFile, String referenceFile, 
            Filter inProcessFilter, Filter postProcessFilter, int maxDepth, 
            int maxSites, boolean removeDuplicates, int removeFlags, 
            long seed, boolean depthSampling) 
                    throws Exception {
        this.inputFile = inputFile;
        this.indexFile = indexFile;
        this.referenceFile = referenceFile;
        this.inProcessFilter = inProcessFilter;
        this.postProcessFilter = postProcessFilter;
        this.maxDepth = maxDepth;
        this.maxSites = maxSites;
        this.removeDuplicates = removeDuplicates;
        this.removeFlags = removeFlags;
        this.seed = seed;
        this.depthSampling = depthSampling;
        this.random = new Random(this.seed);
    }
    
    private boolean validate() {
        boolean ok = true;
        if (!Validator.validateFileExists("reference_file", referenceFile, true)) {
            ok = false;
        }
        if (!Validator.validateFileExists("input_file", inputFile, true)) {
            ok = false;
        }
        if (!Validator.validateFileExists("index_file", indexFile, false)) {
            ok = false;
        }
        return ok;
    }
    
    public void scan() throws Exception  {       
        System.out.println(new Date() + " Initializing...");
        
        boolean good = validate();
        good &= inProcessFilter.validate();
        good &= postProcessFilter.validate();
        if (!good) {
            return;
        }
        ConfigManager config = ConfigManager.getInstance();
        
        // site object facetory
        int initialMinDepth = 160;
        int initialMaxDepth = Math.max(initialMinDepth, maxDepth);
        SiteObjectManager siteManager = new SiteObjectManager(initialMinDepth, initialMaxDepth);
        
        // reference manager
        ReferenceManager referenceManager = createReferenceManager();
        
        // sam reader
        SAMFileReader samFileReader = new SAMFileReader(
                new File(inputFile), 
                indexFile == null ? null : new File(indexFile));
        samFileReader.setValidationStringency(ValidationStringency.SILENT);
     
        // reads cache
        ReadsCache readsCache = new ReadsCache(config.getInt(
                null, "max_recent_reads" , 100000));
        
        // context
        MosaicHunterContext context = 
                new MosaicHunterContext(samFileReader, referenceManager, readsCache);
        
       
        
        System.out.println(new Date() + " Initializing filters...");
        inProcessFilter.init(context);
        postProcessFilter.init(context);
        
        // scan
        SAMFileReader input = new SAMFileReader(
                new File(inputFile), 
                indexFile == null ? null : new File(indexFile));
        input.setValidationStringency(ValidationStringency.SILENT);
        
        boolean ok = true;
        for (SAMSequenceRecord seq : input.getFileHeader().getSequenceDictionary().getSequences()) {
            int sid = referenceManager.getReferenceId(seq.getSequenceName());
            if (sid >= 0) {
                if (seq.getSequenceLength() == referenceManager.getReferenceLength(sid)) {
                } else {
                    System.out.println("inconsistent reference length: " + seq.getSequenceName());
                    ok = false;
                }
            }
        }
        
        if (!ok) {
            input.close();
            samFileReader.close();
            return;
        }
        
        long processedReads = 0;
        long processedSites = 0;
        long totalSites = 0;
        long startTime = System.currentTimeMillis();
        long depthSum = 0;
        long depthCount = 0;
        
        int minReadQuality = config.getInt(null, "min_read_quality", 0);
        int minMappingQuality = config.getInt(null, "min_mapping_quality", 0);
        
        String chr = config.get(null, "chr", null);
        String inputBedFile = config.get(null, "input_bed_file", null);
        boolean inputSampling = config.getBoolean(null, "input_sampling", false);
        
        List<Region> regions = null;
        if (chr != null && !chr.trim().isEmpty()) {
            int chrId = referenceManager.getReferenceId(chr);
            if (chrId < 0) {
                input.close();
                samFileReader.close();
                throw new Exception("invalid chr parameter");
            }
            int startPosition = config.getInt(null, "start_position", 1);
            int endPosition = config.getInt(null, "end_position", Integer.MAX_VALUE);
            if (endPosition > referenceManager.getReferenceLength(chrId)) {
                endPosition = (int) referenceManager.getReferenceLength(chrId);
            }
            regions = new ArrayList<Region>(Collections.singletonList(
                    new Region(chr, chrId, startPosition, endPosition)));
        } else if (inputBedFile != null && !inputBedFile.trim().isEmpty()) {
            regions = MosaicHunterHelper.readBedFile(inputBedFile, referenceManager);
        } else if (inputSampling) {
            int inputSamplingRegions = config.getInt(null, "input_sampling_regions", 1);
            int inputSamplingSize  = config.getInt(null, "input_sampling_size", 1);
            regions = MosaicHunterHelper.generateRandomRegions(
                    random, inputSamplingRegions, inputSamplingSize, referenceManager);
        }
        if (regions != null) {
            regions = MosaicHunterHelper.sortAndCombineRegions(regions);
            for (Region r : regions) {
                totalSites += r.getEnd() - r.getStart() + 1;
            }
        } else {
            regions = new ArrayList<Region>();
            for (Reference ref : referenceManager.getReferences()) {
                Region region = new Region(
                        ref.getName(),
                        referenceManager.getReferenceId(ref.getName()),
                        1,
                        (int) ref.getLength());
                regions.add(region);
            }
            totalSites = referenceManager.getTotalLength();
        }
        
        List<Site> passedSites = new ArrayList<Site>();
        
        System.out.println(new Date() + " Scanning...");

        StatsManager.start("in_process");
        
        System.out.println(
                new Date() + " -" +
                " Time(s):" + (System.currentTimeMillis() - startTime) / 1000 + 
                " Reads:" + 0 +
                " Sites:" + 0 + "/" + totalSites + 
                " Progress:" + String.format("%.2f", 0.0) + "%");
        
        
        for (Region region : regions) {
            long startPositionId;
            long endPositionId;
            SAMRecordIterator it = null;
            if (region == null) {
                it = input.iterator();
                startPositionId = 0;
                endPositionId = Long.MAX_VALUE;
            } else {
                it = input.queryOverlapping(region.getChr(), region.getStart(), region.getEnd()); 
                startPositionId = getPositionId(region.getChrId(), region.getStart());
                endPositionId = getPositionId(region.getChrId(), region.getEnd()); 
            }
            
            String lastRefName = null;
            int lastRefPos = 0;
            long lastPositionId = 0;      
            
            int readsBufferSize = (1 << 17) - 1;
            SAMRecord[] readsBuffer = new SAMRecord[readsBufferSize + 1];
            int readsBatchSize = 1000;
            long first = 0;
            long last = 0;
            
            PriorityQueue<Long> siteIds = new PriorityQueue<Long>();
            HashMap<Long, Site> sites = new HashMap<Long, Site>();
            
            while (it.hasNext() || first != last) {
                while (last - first < readsBufferSize && it.hasNext()) {
                    SAMRecord read = it.next();
                    readsBuffer[(int) (last & readsBufferSize)] = read;
                    last++;
                    readsCache.cacheRead(read);
                }
                long end = Math.min(first + readsBatchSize, last);
                while (first < end) {
                    SAMRecord read = readsBuffer[(int) (first & readsBufferSize)];
                    processedReads++;
                    first++;
                    if (first % 1000000 == 0) {
                        long startRefPos = 1;
                        if (getRefId(lastPositionId) == getRefId(startPositionId)) {
                            startRefPos = getRefPos(startPositionId);
                        }
                        long done = processedSites + lastRefPos - startRefPos;
                        double progress = (double) done * 100 / (totalSites <= 0 ? 1 : totalSites);
                        System.out.println(
                                new Date() + " -" +
                                " Time(s):" + (System.currentTimeMillis() - startTime) / 1000 + 
                                " Reads:" + processedReads +
                                " Sites:" + done + "/" + totalSites + 
                                " Progress:" + String.format("%.2f", progress) + "%" + 
                                " " + lastRefName + ":" + lastRefPos);
                    }
                    
                    // filter invalid reference
                    if (!read.getReferenceName().equals(lastRefName) && 
                        referenceManager.getReferenceId(read.getReferenceName()) < 0) {
                        continue;
                    }
                    
                    if (read.getDuplicateReadFlag() && removeDuplicates) {
                        continue;
                    }
                    if ((read.getFlags() & removeFlags) != 0) {
                        continue;
                    }
                    if (read.getMappingQuality() < minMappingQuality) {
                        continue;
                    }
                    String refName = read.getReferenceName();
                    int refId = referenceManager.getReferenceId(refName);
                    if (refId < 0) {
                        continue;
                    }
                    
                    lastPositionId = getPositionId(refId, read.getAlignmentStart());
                    lastRefName = read.getReferenceName();
                    lastRefPos = read.getAlignmentStart();
                    byte[] bases = read.getReadBases();
                    for (AlignmentBlock block : read.getAlignmentBlocks()) {
                        int refPos = block.getReferenceStart();
                        for (int i = 0; i < block.getLength(); ++i, ++refPos) {
                            long posId = getPositionId(refId, refPos);
                            if (posId < startPositionId || posId > endPositionId) {
                                continue;
                            }

                            short basePos = (short) (block.getReadStart() + i - 1);
                            if (read.getBaseQualities()[basePos] < minReadQuality) {
                                continue;
                            }
                            int baseId = MosaicHunterHelper.BASE_TO_ID[bases[basePos]];
                            if (baseId < 0) {
                                continue;
                            }
                            Site site = sites.get(posId);
                            if (site == null) {
                                byte ref = referenceManager.getBaseWithCache(refId, refPos);
                                if (ref == 'N') {
                                    continue;
                                }

                                int initialDepth = (int)(depthSum / (depthCount + 1) * 2);
                                initialDepth = Math.max(initialDepth, initialMinDepth);
                                initialDepth = Math.min(initialDepth, initialMaxDepth);
                                site = siteManager.getSite(initialDepth);
                                site.init(refName, refId, refPos, ref, 0, 0, null);
                                sites.put(posId, site);
                                siteIds.add(posId);
                            }
                            site.increaceRealDepth();
                            if (site.getDepth() < maxDepth) {
                                if (site.getDepth() >= site.getMaxDepth()) {
                                    Site newSite = siteManager.getSite(site.getMaxDepth() + 1);
                                    newSite.copy(site);
                                    sites.put(posId, newSite);
                                    siteManager.returnSite(site);
                                    site = newSite;
                                }
                                site.addRead(read, basePos);
                                
                            } else if (depthSampling && random.nextInt(site.getRealDepth()) < maxDepth) {
                               int ii = random.nextInt(maxDepth);
                               site.replaceRead(ii, read, basePos);
                            }
                            
                        }
                    }
                }
                
                boolean hasMoreReads = it.hasNext() || first != last;
                
                for (Iterator<Long> itor = siteIds.iterator(); itor.hasNext();) {
                    long positionId = itor.next();
                    Site site = sites.get(positionId);
                    if (site == null) {
                        System.out.println("ERROR!!! null site. " + 
                                referenceManager.getReference(
                                    getRefId(positionId)).getName() + ":" + getRefPos(positionId));
                        itor.remove();
                        continue;
                    }
                    if (hasMoreReads && positionId >= lastPositionId) {
                        break;
                    }
                    depthSum += site.getDepth();
                    depthCount++;
                    
                    sites.remove(positionId);
                    itor.remove();
                    
                    if (passedSites.size() < maxSites && inProcessFilter.filter(site)) {
                        passedSites.add(site);   
                    } else {
                        siteManager.returnSite(site);
                    }
                }
                //siteManager.printInfo();
                 
                 

            } 
            if (region != null) {
                processedSites += region.getEnd() - region.getStart() + 1;
            } else {
                processedSites = totalSites;
            }
            it.close();
        }
        
        System.out.println(
                new Date() + " -" +
                " Time(s):" + (System.currentTimeMillis() - startTime) / 1000 + 
                " Reads:" + processedReads +
                " Sites:" + totalSites + "/" + totalSites + 
                " Progress:" + String.format("%.2f", 100.0) + "%");
        
        StatsManager.end("in_process");
        
        StatsManager.start("post_process");
        postProcessFilter.filter(passedSites);
        StatsManager.end("post_process");
         
        inProcessFilter.printStats(true);
        postProcessFilter.printStats(false);
        
        input.close();
        samFileReader.close();
        inProcessFilter.close();
        postProcessFilter.close();
        
    }
    
    
  
    
    private long getPositionId(long refId, long refPos) {
        return (refId << 40) + refPos;
    }
    
    private int getRefId(long posId) {
        return (int) (posId >>> 40);
    }
    
    private int getRefPos(long posId) {
        return (int) (posId & ((1L << 40) - 1));
    }
    
    private ReferenceManager createReferenceManager() throws Exception {
        
        String[] validReferences = ConfigManager.getInstance().getValues(
                null, "valid_references", DEFAULT_VALID_REFERENCES);
    
        ReferenceManager referenceManager = null;
        if (ConfigManager.getInstance().getBoolean(null, "enable_reference_cache", false)) {
            String odName = ConfigManager.getInstance().get(null, "output_dir", ".");
            File od = new File(odName);
            if (!od.isDirectory()) {
                od.mkdirs();
            }
            File cacheFile = new File(od, new File(referenceFile + ".cache").getName());
            System.out.println(new Date() + " Reading reference from cache file: " + 
                    cacheFile.getAbsolutePath());
            if (cacheFile.isFile()) {
                ObjectInputStream ois = null;
                try {
                    FileInputStream fis = new FileInputStream(cacheFile);
                    ois = new ObjectInputStream(fis);
                    referenceManager = (ReferenceManager) ois.readObject();
                    ois.close();
                } catch (Exception e) {
                    System.out.println(new Date() + " Cannot read cache file: " + 
                            cacheFile.getAbsolutePath() + " " + e.getMessage());
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
                System.out.println(new Date() + " Reading reference: " + referenceFile);
                referenceManager = new ReferenceManager(referenceFile, validReferences);
                System.out.println(new Date() + " Writing reference to cache file: " + 
                        cacheFile.getAbsolutePath());
                ObjectOutputStream oos = null;
                try {
                    FileOutputStream fos = new FileOutputStream(cacheFile);
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(referenceManager);
                    oos.close(); 
                } catch (Exception e) {
                    System.out.println(new Date() + " Cannot write cache file: " + 
                            cacheFile.getAbsolutePath() + " " + e.getMessage());
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
            referenceManager = new ReferenceManager(referenceFile, validReferences);
        }
        
        return referenceManager;
    }
   
}

