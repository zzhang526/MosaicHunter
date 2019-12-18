package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.AlignmentBlock;
import net.sf.samtools.SAMRecord;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.config.Validator;

public class MisalignedReadsFilter extends BaseFilter {
    
    public static final String DEFAULT_BLAT_PARAM 
            = "-stepSize=5 -repMatch=2253 -minScore=0 -minIdentity=0.5 -noHead";
	public static final String DEFAULT_STAR_PARAM
			= "--scoreGap -2 --scoreGapNoncan 0 --scoreGapGCAG 0 --scoreGapATAC 0";
    public static final double DEFAULT_MAX_MISALIGNMENT_PERCENTAGE = 0.5;
    public static final int DEFAULT_MIN_SIDE_DISTANCE = 15;
    public static final int DEFAULT_MIN_GAP_DISTANCE = 5;
    public static final double DEFAULT_MIN_OVERLAP_PERCENTAGE = 0.9;
    public static final int DEFAULT_MAX_NM = 2;
	public static final boolean DEFAULT_OMIT_MULTIPLE_ALIGNMENT = false;
    public static final boolean DEFAULT_ENABLE_BLAT = true;
	public static final boolean DEFAULT_ENABLE_STAR = false;
	
    private final String blatPath;
	private final String starPath;
    private final String blatParam;
	private final String starParam;
    private final String outputDir;
    private final String tmpInputFaFile;
	private final String tmpInputFqFile;
	private final String tmpOutputStarPrefix;
    private final String tmpOutputPslFile;
    private final String referenceFile;
	private final String starReferenceDir;
    private final double maxMisalignmentPercentage;
    private final int minSideDistance;
    private final int minGapDistance;
    private final double minOverlapPercentage;
	private final int maxNM;
	private final boolean omitMultipleAlignment;
	private final boolean enableBlat;
	private final boolean enableStar;

    public MisalignedReadsFilter(String name) {
        this(name,
             ConfigManager.getInstance().get(name, "blat_path", null),
			 ConfigManager.getInstance().get(name, "star_path", null),
             ConfigManager.getInstance().get(name, "blat_param", DEFAULT_BLAT_PARAM),
			 ConfigManager.getInstance().get(name, "star_param", DEFAULT_STAR_PARAM),
             ConfigManager.getInstance().get(null, "output_dir", "."),
             ConfigManager.getInstance().get(name, "reference_file", "").isEmpty() ?
                     ConfigManager.getInstance().get(null, "reference_file", "") :
                     ConfigManager.getInstance().get(name, "reference_file", ""),
             ConfigManager.getInstance().get(name, "star_reference_dir", null),
             ConfigManager.getInstance().getDouble(
                     name, "max_misalignment_percentage", DEFAULT_MAX_MISALIGNMENT_PERCENTAGE),
             ConfigManager.getInstance().getInt(
                     name, "min_side_distance", DEFAULT_MIN_SIDE_DISTANCE),
             ConfigManager.getInstance().getInt(
                     name, "min_gap_distance", DEFAULT_MIN_GAP_DISTANCE),
             ConfigManager.getInstance().getDouble(
                     name, "min_overlap_percentage", DEFAULT_MIN_OVERLAP_PERCENTAGE),
			 ConfigManager.getInstance().getInt(
                     name, "max_NM", DEFAULT_MAX_NM),
             ConfigManager.getInstance().getBoolean(
                     name, "omit_multiple_alignment", DEFAULT_OMIT_MULTIPLE_ALIGNMENT),
             ConfigManager.getInstance().getBoolean(
                     name, "enable_blat", DEFAULT_ENABLE_BLAT),
             ConfigManager.getInstance().getBoolean(
                     name, "enable_star", DEFAULT_ENABLE_STAR));
    }
    
    public MisalignedReadsFilter(String name, 
            String blatPath, String starPath, String blatParam, String starParam,
			String outputDir, String referenceFile, String starReferenceDir,
            double maxMisalignmentPercentage, int minSideDistance, int minGapDistance, 
            double minOverlapPercentage, int maxNM, boolean omitMultipleAlignment, 
			boolean enableBlat, boolean enableStar) {
        super(name);
        this.blatPath = blatPath;
		this.starPath = starPath;
        this.blatParam = blatParam;
		this.starParam = starParam;        
        this.outputDir = outputDir;
        this.tmpInputFaFile = new File(outputDir, name + ".fa").getPath();
		this.tmpInputFqFile = new File(outputDir, name + ".fq").getPath();
		this.tmpOutputStarPrefix = new File(outputDir, name + ".star").getPath();
        this.tmpOutputPslFile = new File(outputDir, name + ".psl").getPath();
        this.referenceFile = referenceFile;
		this.starReferenceDir = starReferenceDir;
        this.maxMisalignmentPercentage = maxMisalignmentPercentage;
        this.minSideDistance = minSideDistance;
        this.minGapDistance = minGapDistance;
        this.minOverlapPercentage = minOverlapPercentage;
		this.maxNM = maxNM;
		this.omitMultipleAlignment = omitMultipleAlignment;
		this.enableBlat = enableBlat;
		this.enableStar = enableStar;
    }        
    
    @Override
    public boolean validate() {
        boolean ok = true;
        if (enableBlat) {
            if (!Validator.validateExists(getName() + ".blat_path", blatPath, false)) {
                ok = false;     
            } else if (!Validator.validateCommandExists("blat", getBlatCmd())) {
                ok = false; 
            }
        }
		if (enableStar) {
            if (!Validator.validateExists(getName() + ".star_path", starPath, false)) {
                ok = false;     
            } else if (!Validator.validateCommandExists("star", getStarCmd())) {
                ok = false; 
            }
        }
        return ok;
    }
	
    @Override
    public boolean doFilter(Site site) {
        return !doFilter(Collections.singletonList(site)).isEmpty();
    }
    
    @Override
    public List<Site> doFilter(List<Site> sites) {
        //System.out.println(new Date() + " " + getName() + " " + sites.size());
        if (sites.isEmpty()) {
            return sites;
        }
        try {
            Map<String, AlignmentEntry> alignments = null;
            
			if (enableBlat) {
                createFastaFile(sites);
                int result = runBlat();
                if (result != 0) {
                    System.out.println("blat process failed(exit code: " + result + ")");
                    return new ArrayList<Site>();
                }
                alignments = parsePslFile();
            }
			
			if (enableStar) {
                createFastqFile(sites);
                int result = runStar();
                if (result != 0) {
                    System.out.println("star process failed(exit code: " + result + ")");
                    return new ArrayList<Site>();
                }
                alignments = parsePslFile();
            }
            
            List<Site> results = new ArrayList<Site>();
            
            for (Site entry : sites) {
                int misalignmentMajorCount = 0;
                int misalignmentMinorCount = 0;
                int[][] alignmentResultCount = new int[2][AlignmentResult.values().length];
                
                for (int i = 0; i < entry.getDepth(); ++i) {
                    byte base = entry.getBases()[i];
                    if (base != entry.getMajorAllele() && base != entry.getMinorAllele()) {
                        continue;
                    }
                    SAMRecord samRecord = entry.getReads()[i];
                    int readPos = entry.getBasePos()[i];
                    String id = null;
					AlignmentEntry alignment = null;
					
					if (enableBlat) {
						id = samRecord.getReadName();
						if (samRecord.getFirstOfPairFlag()) {
							id += "/1";
						} else {
							id += "/2";
						}
						alignment = alignments.get(id);
					}
					
					if (enableStar) {
						id = samRecord.getReadName();
						if (samRecord.getFirstOfPairFlag()) {
							id += "_1";
						} else {
							id += "_2";
						}
						alignment = alignments.get(id);
					}
					
                    AlignmentResult r = getAlignmentResult(
                            samRecord, entry.getRefName(), readPos, alignment);
                    if (r.equals(AlignmentResult.ALIGNMENT_MISSING) && !enableBlat && !enableStar) {
                        r = AlignmentResult.ALIGNMENT_OK;
                    }
                    
                    alignmentResultCount[base == entry.getMajorAllele() ? 0 : 1][r.ordinal()]++;
                    if (!omitMultipleAlignment) {
						if (r != AlignmentResult.ALIGNMENT_OK) {
							if (base == entry.getMajorAllele()) {
								misalignmentMajorCount++;
							} else {
								misalignmentMinorCount++;
							}
						}
					} else {
						if (r != AlignmentResult.ALIGNMENT_OK && r != AlignmentResult.MULTIPLE_ALIGNMENTS) {
							if (base == entry.getMajorAllele()) {
								misalignmentMajorCount++;
							} else {
								misalignmentMinorCount++;
							}
						}
					}
                }
                
				double p1;
				double p2;
				
				if (entry.getMajorAlleleCount()>0) {
					p1 = (double) misalignmentMajorCount / entry.getMajorAlleleCount();
				} else {
					p1 = 0;
				}

				if (entry.getMinorAlleleCount()>0) {				
					p2 = (double) misalignmentMinorCount / entry.getMinorAlleleCount();
				} else {
					p2 = 0;
				}
                
                List<Object> metadata = new ArrayList<Object>();
                for (AlignmentResult r : AlignmentResult.values()) {
                    String d = r.name() + ":" + alignmentResultCount[0][r.ordinal()] + 
                                          "," + alignmentResultCount[1][r.ordinal()];
                    metadata.add(d);
                }
                metadata.add("misalignment:" + misalignmentMajorCount + 
                                         "," + misalignmentMinorCount);
                metadata.add(p1);
                metadata.add(p2);
                entry.setMetadata(
                        getName(),
                        metadata.toArray());
                
                if (p1 < maxMisalignmentPercentage + 1e-9 && p2 < maxMisalignmentPercentage + 1e-9) {
                    results.add(entry);
                }
            }
             
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<Site>(); 
        }
    }    
    
    private AlignmentResult getAlignmentResult(
            SAMRecord samRecord, String chr, int readPos, AlignmentEntry alignment) {
        if (readPos < minSideDistance || 
            samRecord.getReadLength() - 1 - readPos < minSideDistance) {
            return AlignmentResult.NEAR_SIDE;
        }
        Object nm = samRecord.getAttribute("NM");
        if (nm != null && nm instanceof Integer && (Integer) nm > maxNM) {
            return AlignmentResult.NM;
        }        
        
        for (AlignmentBlock ab : samRecord.getAlignmentBlocks()) {
            int pos = readPos +1;
            if ((pos >= ab.getReadStart() && pos - ab.getReadStart() < minGapDistance) ||
                (pos <= ab.getReadStart() + ab.getLength() - 1 
                 && ab.getReadStart() + ab.getLength() - 1 - pos < minGapDistance)) {
              return AlignmentResult.NEAR_GAP;
            }
        }
        
        if (alignment == null) {
            return AlignmentResult.ALIGNMENT_MISSING;
        }        
        if (alignment.count > 1) {
            return AlignmentResult.MULTIPLE_ALIGNMENTS;
        }
        if (!alignment.chr.equals(chr)) {
            return AlignmentResult.CHROM_MISMATCH;
        }
        
        String[] psl = alignment.psl;
        int blockCount = Integer.parseInt(psl[psl.length - 4]);
        int[] blockSizes = new int[blockCount];
        int[] blockPositions = new int[blockCount];
        String[] blockSizeTokens = psl[psl.length - 3].split(",");
        String[] blockPositionTokens = psl[psl.length - 1].split(",");
        for (int i = 0; i < blockCount; ++i) {
            blockSizes[i] = Integer.parseInt(blockSizeTokens[i]);
            blockPositions[i] = Integer.parseInt(blockPositionTokens[i]);
        }
      
        if (calcOverlap(blockSizes, blockPositions, samRecord) < minOverlapPercentage) {
            return AlignmentResult.ALIGNMENT_MISMATCH;
        } 
        return AlignmentResult.ALIGNMENT_OK;
    }
    
    private double calcOverlap(int[] blockSizes, int[] blockPositions, SAMRecord record) {
        
        int overlap = 0;
        int j = 0;
        for (int i = 1; i <= record.getReadLength() && j < blockSizes.length; ++i) {
            int refPos = record.getReferencePositionAtReadPosition(i);
            if (refPos >= blockPositions[j] && refPos < blockPositions[j] + blockSizes[j]) {
                overlap++;
            } else if (refPos >= blockPositions[j] + blockSizes[j]) {
                j++;
                i--;
            }
        }
        
        return (double) overlap / record.getReadLength();
    }
    
    private void createFastaFile(List<Site> sites) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpInputFaFile));
        Set<String> done = new HashSet<String>();
        for (Site site : sites) {
            for (int i = 0; i < site.getDepth(); ++i) {
                SAMRecord samRecord = site.getReads()[i];         
                String id = samRecord.getReadName();
                if (samRecord.getFirstOfPairFlag()) {
                    id += "/1";
                } else {
                    id += "/2";
                }                
                if (AlignmentResult.ALIGNMENT_MISSING != 
                    getAlignmentResult(
                            samRecord, site.getRefName(), 
                            site.getBasePos()[i], null)) {
                    continue;
                }
  
                if (done.contains(id)) {
                    continue;
                }
                done.add(id);
                writer.write('>');
                writer.write(id);
                writer.newLine();
                writer.write(samRecord.getReadString());
                writer.newLine();                
            }
        }
        writer.close();
    }
	
	private void createFastqFile(List<Site> sites) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpInputFqFile));
        Set<String> done = new HashSet<String>();
        for (Site site : sites) {
            for (int i = 0; i < site.getDepth(); ++i) {
                SAMRecord samRecord = site.getReads()[i];         
                String id = samRecord.getReadName();
                if (samRecord.getFirstOfPairFlag()) {
                    id += "_1";
                } else {
                    id += "_2";
                }                
                if (AlignmentResult.ALIGNMENT_MISSING != 
                    getAlignmentResult(
                            samRecord, site.getRefName(), 
                            site.getBasePos()[i], null)) {
                    continue;
                }
  
                if (done.contains(id)) {
                    continue;
                }
                done.add(id);
                writer.write('@');
                writer.write(id);
                writer.newLine();
                writer.write(samRecord.getReadString());
                writer.newLine();  
				writer.write('+');
				writer.newLine();
				writer.write(samRecord.getBaseQualityString());
				writer.newLine();
            }
        }
        writer.close();
    }
    
    private String getBlatCmd() {
        if (blatPath == null || blatPath.trim().isEmpty()) {
            return "blat";
        }
        File pathFile = new File(blatPath);
        if (pathFile.isDirectory()) {
            return new File(blatPath, "blat").getAbsolutePath();
        } else if (pathFile.isFile()) {
            return pathFile.getAbsolutePath();
        } else {
            return null;
        }
    }
	
	private String getStarCmd() {
        if (starPath == null || starPath.trim().isEmpty()) {
            return "STAR";
        }
        File pathFile = new File(starPath);
        if (pathFile.isDirectory()) {
            return new File(starPath, "STAR").getAbsolutePath();
        } else if (pathFile.isFile()) {
            return pathFile.getAbsolutePath();
        } else {
            return null;
        }
    }
    
    private int runBlat() throws IOException, InterruptedException {
        String blatCmd = getBlatCmd();
        String cmd = blatCmd + " " + blatParam + " " + 
                referenceFile + " " + tmpInputFaFile + " " + tmpOutputPslFile;
        System.out.println("run blat: " + cmd);
        Runtime rt = Runtime.getRuntime();
        Process blat = rt.exec(cmd);
        return blat.waitFor();
    }
	
	private int runStar() throws IOException, InterruptedException {
        String starCmd = getStarCmd();
        String cmd = starCmd + " " + starParam + " --genomeDir " + starReferenceDir +
		" --readFilesIn " + tmpInputFqFile + " --outFileNamePrefix " + tmpOutputStarPrefix;
        System.out.println("run star: " + cmd);
        Runtime rt = Runtime.getRuntime();
        Process star = rt.exec(cmd);
		star.waitFor();
		String cmd2 = "sam2psl.py -i " + tmpOutputStarPrefix + "Aligned.out.sam -o " + tmpOutputPslFile;
		System.out.println("convert sam to psl: " + cmd2);
		Runtime rt2 = Runtime.getRuntime();
		Process convert = rt2.exec(cmd2);	
        return convert.waitFor();
    }
    
    private Map<String, AlignmentEntry> parsePslFile() throws IOException {
        Map<String, AlignmentEntry> alignments = new HashMap<String, AlignmentEntry>();
        BufferedReader reader = null; 
        try {
            reader = new BufferedReader(new FileReader(tmpOutputPslFile));
            for(;;) {
                 String line =  reader.readLine();
                 if (line == null) {
                     break;
                 }
                 String[] tokens = line.split("\\t");
                 
                 AlignmentEntry entry = new AlignmentEntry();
                 String id = tokens[9];
                 entry.chr = tokens[13];
                 if (entry.chr.startsWith("chr")) {
                     entry.chr = entry.chr.substring(3); 
                 }
                 entry.score = Integer.parseInt(tokens[0]) +
                               Integer.parseInt(tokens[2]) - 
                               Integer.parseInt(tokens[1]) - 
                               Integer.parseInt(tokens[4]) - 
                               Integer.parseInt(tokens[6]);
                 entry.count = 1;
                 entry.psl = tokens;
                 AlignmentEntry entry2 = alignments.get(id);
                 if (entry2 == null || entry.score > entry2.score) {
                     alignments.put(id, entry);    
                 } else if (entry.score == entry2.score) {
                     entry2.count++;
                 }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return alignments;
    }
    
    private class AlignmentEntry {
        private String chr;
        private int score;
        private int count;
        private String[] psl;
    }
    
    public enum AlignmentResult {
        ALIGNMENT_OK,
        ALIGNMENT_MISSING,
        MULTIPLE_ALIGNMENTS,
        CHROM_MISMATCH,
        ALIGNMENT_MISMATCH,
        NEAR_SIDE,
        NEAR_GAP,
        NM
    }
    
}
