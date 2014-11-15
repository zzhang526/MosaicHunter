package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import cn.edu.pku.cbi.mosaichunter.BamSiteReader;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.FishersExactTest;

public class MosaicFilter extends BaseFilter {

    public static final int DEFAULT_MAX_DEPTH = 500;
    public static final int DEFAULT_ALPHA_PARAM = 0;
    public static final int DEFAULT_BETA_PARAM = 0;
    public static final int DEFAULT_MIN_READ_QUALITY = 20;
    public static final int DEFAULT_MIN_MAPPING_QUALITY = 20;

    public static final String DEFAULT_SEX = "M";
    public static final double DEFAULT_MOSAIC_THRESHOLD = 0.05;
    public static final double DEFAULT_CASE_THRESHOLD = 0.05;
    public static final double DEFAULT_CONTROL_THRESHOLD = 0.5;
    public static final double DEFAULT_CONTROL_FISHER_THRESHOLD = 0.01;

    public static final int MAX_QUALITY = 64;
    public static final int MAX_QUALITY_DEPTH = 70;
    
    public static final double DE_NOVO_RATE = 1e-8;
    public static final double MOSAIC_RATE = 1e-7;
    public static final double UNKNOWN_AF = 0.002;
    public static final double DEFAULT_AF = 1e-4; 
    public static final double LOGZERO = -1e100;
    
    public static final double[] DEFAULT_BASE_CHANGE_RATE = new double[] {
        1, 1, 1, 1,
        1, 1, 1, 1,
        1, 1, 1, 1,
        1, 1, 1, 1
    }; // A, C, G, T

    private double[][] beta = null;
    private static double[][] c = null;

    private final int maxDepth;
    private final int alphaParam;
    private final int betaParam;
    private final int minReadQuality;
    private final int minMappingQuality;
    private final String sex;
    private final double[] baseChangeRate;
    private final double mosaicThreshold;
    private final boolean trio;
    private final String fatherBamFile;
    private final String fatherIndexFile;
    private final String motherBamFile;
    private final String motherIndexFile;
    
    private final boolean control;
    private final String controlBamFile;
    private final String controlIndexFile;
    private final double controlThreshold;
    private final double caseThreshold;
    private final boolean controlFisher;
    private final double controlFisherThreshold;
    private final boolean heterozygous;
    
    private DbSnpReader dbSnpReader = null;
    private BamSiteReader fatherSiteReader = null;
    private BamSiteReader motherSiteReader = null; 
    private BamSiteReader controlSiteReader = null; 
    private final double[][] defaultAF = new double[4][4];    
    
    public MosaicFilter(String name) {
        this(name, ConfigManager.getInstance().getInt(null, "max_depth", DEFAULT_MAX_DEPTH), 
                   ConfigManager.getInstance().getInt(null, "min_read_quality", DEFAULT_MIN_READ_QUALITY),
                   ConfigManager.getInstance().getInt(null, "min_mapping_quality", DEFAULT_MIN_MAPPING_QUALITY),
                   ConfigManager.getInstance().getInt(name, "alpha_param", DEFAULT_ALPHA_PARAM), 
                   ConfigManager.getInstance().getInt(name, "beta_param", DEFAULT_BETA_PARAM), 
                   ConfigManager.getInstance().get(name, "sex", DEFAULT_SEX),
                   ConfigManager.getInstance().getDoubles(name, "base_change_rate", DEFAULT_BASE_CHANGE_RATE),
                   ConfigManager.getInstance().getDouble(name, "mosaic_threshold", DEFAULT_MOSAIC_THRESHOLD),
                   ConfigManager.getInstance().getBoolean(name, "heterozygous", false),
                   ConfigManager.getInstance().getBoolean(name, "trio", false),
                   ConfigManager.getInstance().get(name, "father_bam_file", null),
                   ConfigManager.getInstance().get(name, "father_index_file", null),
                   ConfigManager.getInstance().get(name, "mother_bam_file", null),
                   ConfigManager.getInstance().get(name, "mother_index_file", null),
                   ConfigManager.getInstance().getBoolean(name, "control", false),
                   ConfigManager.getInstance().get(name, "control_bam_file", null),
                   ConfigManager.getInstance().get(name, "control_index_file", null),
                   ConfigManager.getInstance().getDouble(name, "case_threshold", DEFAULT_CASE_THRESHOLD),
                   ConfigManager.getInstance().getDouble(name, "control_threshold", DEFAULT_CONTROL_THRESHOLD),
                   ConfigManager.getInstance().getBoolean(name, "control_fisher", false),
                   ConfigManager.getInstance().getDouble(name, "control_fisher_threshold", DEFAULT_CONTROL_FISHER_THRESHOLD)
                );
    }

    public MosaicFilter(String name, 
            int maxDepth, int minReadQuality, int minMappingQuality, int alphaParam, int betaParam,
            String sex, double[] baseChangeRate, double mosaicThreshold,
            boolean heterozygous, boolean trio, 
            String fatherBamFile, String fatherIndexFile, 
            String motherBamFile, String motherIndexFile,
            boolean control, 
            String controlBamFile, String controlIndexFile,
            double caseThreshold, double controlThreshold,
            boolean controlFisher, double controlFisherThreshold) {
        super(name);
        this.maxDepth = maxDepth;
        this.alphaParam = alphaParam;
        this.betaParam = betaParam;
        this.minReadQuality = minReadQuality;
        this.minMappingQuality = minMappingQuality;
        this.sex = sex;
        this.baseChangeRate = baseChangeRate;
        this.mosaicThreshold = mosaicThreshold;
        this.heterozygous = heterozygous;
        this.trio = trio;
        this.fatherBamFile = fatherBamFile;
        this.fatherIndexFile = fatherIndexFile;
        this.motherBamFile = motherBamFile;
        this.motherIndexFile = motherIndexFile;
        this.control = control;
        this.controlBamFile = controlBamFile;
        this.controlIndexFile = controlIndexFile;
        this.caseThreshold = caseThreshold;
        this.controlThreshold = controlThreshold;
        this.controlFisher = controlFisher;
        this.controlFisherThreshold = controlFisherThreshold;
               
    }

    @Override
    public void init() throws Exception {
        super.init();
        if (beta == null) {
            initDbSnp();   
            initTrio(); 
            initBeta();          
        }               
    }

    private void initTrio() throws Exception {
        if (control || controlFisher) {
            if (controlBamFile == null) {
                throw new Exception(getName() + ".control_bam_file property is missing");
            }
            controlSiteReader = new BamSiteReader(
                    controlBamFile, controlIndexFile, maxDepth, minReadQuality, minMappingQuality);
            controlSiteReader.init();
        } else if (trio) {
            if (fatherBamFile == null) {
                throw new Exception(getName() + ".father_bam_file property is missing");
            }
            if (motherBamFile == null) {
                throw new Exception(getName() + ".mother_bam_file property is missing");
            }
            
            fatherSiteReader = new BamSiteReader(
                    fatherBamFile, fatherIndexFile, maxDepth, minReadQuality, minMappingQuality);
            fatherSiteReader.init();
            
            motherSiteReader = new BamSiteReader(
                    motherBamFile, motherIndexFile, maxDepth, minReadQuality, minMappingQuality);
            motherSiteReader.init();
        }
    }
    
    private void initBeta() {
        int maxI = maxDepth + 1;
        int maxJ = maxDepth + 3;
        if (alphaParam > 0 && betaParam > 0) {
            maxI = maxDepth + alphaParam + betaParam + 1;
            maxJ = maxDepth + alphaParam + betaParam + 3;
        }
        beta = new double[maxI][maxJ];
        c = new double[maxI + 2][maxJ + 1];

        // Beta(P,Q) = (P+Q)/(P*Q*C(P+Q,P)) = 1/(Q*C(P+Q-1,P-1))
        for (int i = 1; i < maxI + 2; ++i) {
          c[i][0] = 0;
          c[i][i] = 0;
          for (int j = 1; j <= i / 2; ++j) {
            c[i][j] = expAdd(c[i - 1][j - 1], c[i - 1][j]);
            c[i][i - j] = c[i][j];
          }
        }
        double[] logQ = new double[maxI + 2];
        for (int i = 0; i < maxI + 2; ++i) {
            logQ[i] = log10(i);
        }
        for (int i = 0; i < maxI; ++i) {
            for (int j = 0; j <= i + 1; ++j) {
                int p = j + 1;
                int q = i + 3 - p;
                beta[i][j] = -logQ[q] - c[p + q - 1][p - 1];
            }
        }
    }
    
    private void initDbSnp() throws Exception {        
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                defaultAF[i][j] = i == j ? 1 : DEFAULT_AF;
            }
        }        
        
        String dbSnpFile = ConfigManager.getInstance().get(getName(), "dbsnp_file", null);
        if (dbSnpFile != null && !dbSnpFile.trim().isEmpty()) {
            BufferedReader reader = new BufferedReader(new FileReader(dbSnpFile));
            dbSnpReader = new DbSnpReader(reader); 
        }
    }     
    
    @Override
    public void close() throws Exception {
        if (dbSnpReader != null && dbSnpReader.reader != null) {
            dbSnpReader.reader.close();
            dbSnpReader.reader = null;
        }
        if (fatherSiteReader != null) {
            fatherSiteReader.close();
            fatherSiteReader = null;
        }
        if (motherSiteReader != null) {
            motherSiteReader.close();
            motherSiteReader = null;
        }
    }

    public static long[] cnt = new long[255];
  
    
    @Override
    public boolean doFilter(FilterEntry filterEntry) {
           
        if (filterEntry.getDepth() == 0) {
            return false;
        }
        
        boolean pass = false;
        if (control || controlFisher) {
            pass = calcMosaicWithControl(filterEntry);
        } else if (heterozygous) {
           double het = calcHeterozygous(filterEntry);
           pass = het > mosaicThreshold;
        } else {
            double mosaic = trio ? 
                    calcTrioMosaic(filterEntry) : calcIndividualMosaic(filterEntry);
            pass = mosaic > mosaicThreshold;
        }
        
        return pass;
        
    }
    
    private boolean calcMosaicWithControl(FilterEntry filterEntry) {
        FilterEntry control;
        try {
            control = controlSiteReader.read(
                    filterEntry.getChrName(), filterEntry.getRefPos(), filterEntry.getRef(), 
                    filterEntry.getAlleleCountOrder());
        } catch (Exception e) {
            cnt[201]++;
            return false;
        } 
        if (control.getDepth() == 0) {
            cnt[207]++;
            return false;
        }
        
        double[] casePosterior = calcIndividualPosterior(filterEntry, false);
        double[] controlPosterior = calcIndividualPosterior(control, false);
        if (casePosterior == null) {
            cnt[202]++;
            return false;
        }
        if (controlPosterior == null) {
            cnt[203]++;
            return false;
        }
        
        int[] alleleCounts = new int[4];
        int majorId = filterEntry.getMajorAlleleId();
        int minorId = filterEntry.getMinorAlleleId();
        alleleCounts[filterEntry.getMajorAlleleId()] = 1;
        if (casePosterior[0] < casePosterior[1] || casePosterior[0] < casePosterior[3]) {
            alleleCounts[filterEntry.getMinorAlleleId()] = 1;
        }
        alleleCounts[control.getMajorAlleleId()] = 1;
        if (control.getMajorAlleleId() != majorId) {
            minorId = control.getMajorAlleleId();
        }
        if (controlPosterior[0] < controlPosterior[1] || controlPosterior[0] < controlPosterior[3]) {
            alleleCounts[control.getMinorAlleleId()] = 1;
            if (control.getMinorAlleleId() != majorId) {
                minorId = control.getMinorAlleleId();
            }
        }
        if (alleleCounts[0] + alleleCounts[1] + alleleCounts[2] + alleleCounts[3] > 2) {
            cnt[204]++;
            return false;
        }
        
        byte majorAllele = (byte) MosaicHunterHelper.idToBase(majorId);
        byte minorAllele = (byte) MosaicHunterHelper.idToBase(minorId);
        
        int a = filterEntry.getAlleleCount(majorAllele);
        int b = filterEntry.getAlleleCount(minorAllele);
        
        
        /**************/
        //System.out.println(majorId + " " + (char) getBase(majorId) + " " + control.getAlleleCount(majorAllele));
        //System.out.println(minorId + " " + (char) getBase(minorId) + " " + control.getAlleleCount(minorAllele));
        //System.out.println(control.getAlleleCount()+" "+control.getAlleleCount(1)+" "+control.getAlleleCount(2)+" "+control.getAlleleCountById(3));
        //System.out.println(control.getAlleleCountOrder());
        //System.out.println(filterEntry.getAlleleCount(0)+" "+filterEntry.getAlleleCount(1)+" "+
        //filterEntry.getAlleleCount(2)+" "+filterEntry.getAlleleCount(3));
        //System.out.println(filterEntry.getAlleleCountOrder());
        
        
        
        
        int c = control.getAlleleCount(majorAllele);
        int d = control.getAlleleCount(minorAllele);
        
        if (controlFisher) {
            double p = FishersExactTest.twoSided(a, b, c, d);
            filterEntry.setMetadata(
                    getName(),
                    new Object[] {
                        "Case(" + (char) getBase(majorId) + ":" + a + ","
                                + (char) getBase(minorId) + ":" + b + ")",
                        "Control(" + (char) getBase(majorId) + ":" + c + ","
                                + (char) getBase(minorId) + ":" + d + ")",
                        casePosterior[0], casePosterior[1], casePosterior[2], casePosterior[3],
                        controlPosterior[0], controlPosterior[1], controlPosterior[2], controlPosterior[3],
                        p});
            return p < controlFisherThreshold;
        }
        
        if (filterEntry.getMajorAlleleId() != majorId || filterEntry.getMinorAlleleId() != minorId) {
            casePosterior = calcIndividualPosterior(filterEntry, false, majorId, minorId);
        }
        
        if (control.getMajorAlleleId() != majorId || control.getMinorAlleleId() != minorId) {
            controlPosterior = calcIndividualPosterior(control, false, majorId, minorId);
        }
        if (casePosterior == null) {
            cnt[205]++;
            return false;
        }
        if (controlPosterior == null) {
            cnt[206]++;
            return false;
        }
        
        int controlGenotype = 0;
        for (int i = 1; i < 4; ++i) {
            if (controlPosterior[controlGenotype] < controlPosterior[i]) {
                controlGenotype = i;
            }
        }
        double p = Math.pow(10, controlPosterior[controlGenotype]);
        
        double q = 0;
        for (int i = 0; i < 4; ++i) {
            if (i != controlGenotype) {
                q += Math.pow(10, casePosterior[i]);
            }
        }
        
        filterEntry.setMetadata(
                getName(),
                new Object[] {
                    "Case(" + (char) getBase(majorId) + ":" + a + ","
                            + (char) getBase(minorId) + ":" + b + ")",
                    "Control(" + (char) getBase(majorId) + ":" + c + ","
                            + (char) getBase(minorId) + ":" + d + ")",
                    casePosterior[0], casePosterior[1], casePosterior[2], casePosterior[3],
                    controlPosterior[0], controlPosterior[1], controlPosterior[2], controlPosterior[3],
                    q, p});
        
        return q > caseThreshold && p > controlThreshold;
    }
    
    private double calcTrioMosaic(FilterEntry child) {
        FilterEntry father;
        try {
            father = fatherSiteReader.read(
                    child.getChrName(), child.getRefPos(), child.getRef(), 
                    child.getAlleleCountOrder());
        } catch (Exception e) {
            cnt[101]++;
            return -1;
        } 
        
        FilterEntry mother;
        try {

            mother = motherSiteReader.read(
                    child.getChrName(), child.getRefPos(), child.getRef(),
                    child.getAlleleCountOrder());

        } catch (Exception e) {
            cnt[102]++;
            return -1;
        } 
        
        //System.out.println(this.buildOutput(father));
        //System.out.println(this.buildOutput(mother));
        
       
        if (father == null) {
            cnt[103]++;
            return -1;
        }
        if (mother == null) {
            cnt[104]++;
            return -1;
        }
        
        if (father.getDepth() == 0) {
            cnt[108]++;
            return -1;
        }
        if (mother.getDepth() == 0) {
            cnt[109]++;
            return -1;
        }
        double[] af = getAF(child);
        if (af == null) {
            cnt[105]++;
            return -1;
        }
        
        double[] fatherPrior = calcPrior(
                af, af, father, "M", father.getMajorAlleleId(), father.getMinorAlleleId());
        double[] motherPrior = calcPrior(
                af, af, mother, "F", mother.getMajorAlleleId(), mother.getMinorAlleleId());

        if (motherPrior == null) {
            cnt[106]++;
            motherPrior = new double[] {0, 0, 0 ,0};
        }
        print10(fatherPrior, "father prior");
        print10(motherPrior, "mother prior");

        double[] fatherLikelihood = calcLikelihood(
                father, father.getMajorAlleleId(), father.getMinorAlleleId());
        double[] motherLikelihood = calcLikelihood(
                mother, mother.getMajorAlleleId(), mother.getMinorAlleleId());
        double[] childLikelihood = calcLikelihood(
                child, child.getMajorAlleleId(), child.getMinorAlleleId());
        
        print10(fatherLikelihood, "father like");
        print10(motherLikelihood, "mother like");
        print10(childLikelihood, "child like");
        
        double[][][] cpd = calcCPD(father, mother, child);
        if (cpd == null) {
            cnt[107]++;
            return -1;
        }
        
        double sum = LOGZERO;
        double[][][] joint = new double[4][4][4];
        double[] fatherPosterior = new double[] {LOGZERO, LOGZERO, LOGZERO, LOGZERO};
        double[] motherPosterior = new double[] {LOGZERO, LOGZERO, LOGZERO, LOGZERO};
        double[] childPosterior = new double[] {LOGZERO, LOGZERO, LOGZERO, LOGZERO};
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                for (int k = 0; k < 4; ++k) {
                    joint[i][j][k] = 
                            fatherLikelihood[i] + motherLikelihood[j] + childLikelihood[k] +
                            fatherPrior[i] + motherPrior[j] +
                            cpd[i][j][k];
                    fatherPosterior[i] = expAdd(fatherPosterior[i], joint[i][j][k]);
                    motherPosterior[j] = expAdd(motherPosterior[j], joint[i][j][k]);
                    childPosterior[k] = expAdd(childPosterior[k], joint[i][j][k]);
                    sum = expAdd(sum, joint[i][j][k]);
                }
                print10(joint[i][j], "joint " + i + " " + j);
            }
        }
        print10(fatherPosterior, "father1");
        print10(motherPosterior, "mother1");
        print10(childPosterior, "child1");
        //System.out.println(sum);
        for (int i = 0; i < 4; ++i) {
            fatherPosterior[i] -= sum;
            motherPosterior[i] -= sum;
            childPosterior[i] -= sum;
        }
        
        print10(fatherPosterior, "father");
        print10(motherPosterior, "mother");
        print10(childPosterior, "child");
        
        double mosaic = Math.pow(10, childPosterior[3]);

        child.setMetadata(
                getName(),
                new Object[] {
                    (char) child.getMajorAllele() + "(" + formatAF(af[child.getMajorAlleleId()]) + "):" + 
                    (char) child.getMinorAllele() + "(" + formatAF(af[child.getMinorAlleleId()]) + ")",
                    "F(" + (char) father.getMajorAllele() + ":" + father.getMajorAlleleCount() + ","
                         + (char) father.getMinorAllele() + ":" + father.getMinorAlleleCount() + ")",
                    "M(" + (char) mother.getMajorAllele() + ":" + mother.getMajorAlleleCount() + ","
                         + (char) mother.getMinorAllele() + ":" + mother.getMinorAlleleCount() + ")",
                         
                    childLikelihood[0], childLikelihood[1], childLikelihood[2], childLikelihood[3], 
                    fatherPosterior[0], fatherPosterior[1], fatherPosterior[2], fatherPosterior[3], 
                    motherPosterior[0], motherPosterior[1], motherPosterior[2], motherPosterior[3], 
                    childPosterior[0], childPosterior[1], childPosterior[2], childPosterior[3],
                    mosaic});
        //System.out.println(this.buildOutput(child));
        if (childLikelihood[1] > childLikelihood[3]) {
            return -1;
        }
        return mosaic; 
    }
    
    private double[][] calcBaseProb(FilterEntry individual) {
        double[][] prob = new double[4][];
        for (int genotype = 0; genotype < 4; ++genotype) {
            double[] p = new double[] {
                    DE_NOVO_RATE / 3, DE_NOVO_RATE / 3, DE_NOVO_RATE / 3, DE_NOVO_RATE / 3
            };
            if (genotype == 0) {
                // hom ref
                p[individual.getMajorAlleleId()] = 1.0 - DE_NOVO_RATE;
            } else if (genotype == 1) {
                // het
                p[individual.getMajorAlleleId()] = 0.5 - DE_NOVO_RATE / 3;
                p[individual.getMinorAlleleId()] = 0.5 - DE_NOVO_RATE / 3;
            } else if (genotype == 2) {
                // hom alt
                p[individual.getMinorAlleleId()] = 1.0 - DE_NOVO_RATE;
            } else {
                // mosaic
                double mosaicAF = (double) individual.getMinorAlleleCount() / 
                        (individual.getMajorAlleleCount() + individual.getMinorAlleleCount());
                // TODO
                if (individual.getMinorAlleleCount() == 0) {
                    mosaicAF = MOSAIC_RATE;
                }
                p[individual.getMajorAlleleId()] = (1 - mosaicAF) * (1 - DE_NOVO_RATE / 3 * 2);
                p[individual.getMinorAlleleId()] = mosaicAF * (1 - DE_NOVO_RATE / 3 * 2);
            }
            prob[genotype] = p;
        }
        return prob;
    }
    /*
    private double[] calcChildBaseProb(double[] fatherBaseProb, double[] motherBaseProb, 
            FilterEntry child) {
        String chr = child.getChrName();
        int majorAlleleId = child.getMajorAlleleId();
        int minorAlleleId = child.getMinorAlleleId();
        
        double[] p = new double[4];

        if ((!chr.equals("X") && !chr.equals("Y")) ||
            (chr.equals("X") && sex.equals("F"))) {
            p[0] = fatherBaseProb[majorAlleleId] * motherBaseProb[majorAlleleId];
            p[1] = fatherBaseProb[majorAlleleId] * motherBaseProb[minorAlleleId] +
                   fatherBaseProb[minorAlleleId] * motherBaseProb[majorAlleleId];
            p[2] = fatherBaseProb[minorAlleleId] * motherBaseProb[minorAlleleId];   
        } else if (chr.equals("X") && sex.equals("M")) {
            p[0] = motherBaseProb[majorAlleleId];
            p[1] = 0;
            p[2] = motherBaseProb[minorAlleleId];
        } else if (chr.equals("Y") && sex.equals("M")) {
            p[0] = fatherBaseProb[majorAlleleId];
            p[1] = 0;
            p[2] = fatherBaseProb[minorAlleleId];
        } else {
            return null;
        }
        // normalize
        double sum = (1 - MOSAIC_RATE) / (p[0] + p[1] + p[2]);
        p[0] *= sum;
        p[1] *= sum;
        p[2] *= sum;
        
        p[3] = MOSAIC_RATE;
        
        return p;
    }*/
    
    
    private double[][][] calcCPD(FilterEntry father, FilterEntry mother, FilterEntry child) {
        double[][] fatherBaseProb = calcBaseProb(father);
        double[][] motherBaseProb = calcBaseProb(mother);
        
        double[][][] cpd = new double[4][4][4];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {                    
                cpd[i][j] = calcChildBaseProb(
                        fatherBaseProb[i][child.getMajorAlleleId()], 
                        fatherBaseProb[i][child.getMinorAlleleId()], 
                        motherBaseProb[j][child.getMajorAlleleId()], 
                        motherBaseProb[j][child.getMinorAlleleId()], 
                        child.getChrName(),
                        sex);
                if (cpd[i][j] == null) {
                    return null;
                }
                print10(cpd[i][j], "cpd " + i + " " + j);
               
            }
        }
        return cpd;
    }
    
    
    private double calcIndividualMosaic(FilterEntry filterEntry) {
        double[] posterior = calcIndividualPosterior(filterEntry, true);
        if (posterior == null) {
            return -1;
        }
        double mosaic = Math.pow(10, posterior[3]);
        return mosaic;
    }
    
    private double calcHeterozygous(FilterEntry filterEntry) {
        
        int majorId = filterEntry.getMajorAlleleId();
        int minorId = filterEntry.getMinorAlleleId();
        
        double[] af = getAF(filterEntry);
        if (af == null) {
            return 0;
        }

        double[] prior = calcPrior(af, af, filterEntry, sex, majorId, minorId);
        double[] likelihood = calcLikelihood(filterEntry, majorId, minorId, false); 
        double[] posterior = calcPosterior(majorId, minorId, likelihood, prior);
        if (posterior == null) {
            return 0;
        }
  
        filterEntry.setMetadata(
            getName(),
            new Object[] {
                (char) getBase(majorId) + "(" + formatAF(af[majorId]) + "):" + 
                (char) getBase(minorId) + "(" + formatAF(af[minorId]) + ")",
                prior[0], prior[1], prior[2], 
                likelihood[0], likelihood[1], likelihood[2], 
                posterior[0], posterior[1], posterior[2],
                Math.pow(10, posterior[1])});

        double het = Math.pow(10, posterior[1]);
        return het;
    }

    
    private double[] calcIndividualPosterior(FilterEntry filterEntry, boolean setMetadata) {
        byte majorAllele = filterEntry.getMajorAllele();
        byte minorAllele = filterEntry.getMinorAllele();
        int majorId = getBaseId((char) majorAllele);
        int minorId = getBaseId((char) minorAllele);
        return calcIndividualPosterior(filterEntry, setMetadata, majorId, minorId);
    }
    
    private double[] calcIndividualPosterior(
            FilterEntry filterEntry, boolean setMetadata, int majorId, int minorId) {
        
        double[] af = getAF(filterEntry);
        if (af == null) {
            return null;
        }

        double[] prior = calcPrior(af, af, filterEntry, sex, majorId, minorId);
        double[] likelihood = calcLikelihood(filterEntry, majorId, minorId);  
        double[] posterior = calcPosterior(majorId, minorId, likelihood, prior);
        if (posterior == null) {
            return null;
        }
        
        if (setMetadata) {
            filterEntry.setMetadata(
                getName(),
                new Object[] {
                    (char) getBase(majorId) + "(" + formatAF(af[majorId]) + "):" + 
                    (char) getBase(minorId) + "(" + formatAF(af[minorId]) + ")",
                    prior[0], prior[1], prior[2], prior[3], 
                    likelihood[0], likelihood[1], likelihood[2], likelihood[3], 
                    posterior[0], posterior[1], posterior[2], posterior[3],
                    Math.pow(10, posterior[3])});
        }
        return posterior;
    }
    
    private void print(double[] d, String name) {
        if (true) return;
        if (d != null) System.out.println(name + "\t" + 
    format.format(Math.pow(10,d[0])) + "\t" +
    format.format(Math.pow(10,d[1])) + "\t" +
            format.format(Math.pow(10,d[2])) + "\t" +
                    format.format(Math.pow(10,d[3])) + "\t" );
       

    }
    private void print10(double[] d, String name) {
        
        if (true) return;
        if (d != null) System.out.println(name + "\t" + 
                format.format(d[0]) + "\t" +
                format.format(d[1]) + "\t" +
                format.format(d[2]) + "\t" +
                format.format(d[3]) + "\t" );
       

    }
    
    private double[] calcPrior(
            double[] fatherAf, double[] motherAf, 
            FilterEntry entry, String sex,
            int majorAlleleId, int minorAlleleId) {
        double fatherMajorAf = fatherAf[majorAlleleId] / 
                (fatherAf[majorAlleleId] + fatherAf[minorAlleleId]);
        double motherMajorAf = motherAf[majorAlleleId] / 
                (motherAf[majorAlleleId] + motherAf[minorAlleleId]);
        return calcChildBaseProb(
                fatherMajorAf, 1 - fatherMajorAf, 
                motherMajorAf, 1 - motherMajorAf, 
                entry.getChrName(), sex);
    }
    
    private double[] calcChildBaseProb(
            double fatherMajorAf, double fatherMinorAf,
            double motherMajorAf, double motherMinorAf,
            String chr, String sex) {
        
        double homMajor = 0;
        double het = 0;
        double homMinor = 0;        
        double mosaic = 0;
        if ((!chr.equals("X") && !chr.equals("Y")) ||
            (chr.equals("X") && sex.equals("F"))) {
            homMajor = fatherMajorAf * motherMajorAf;
            homMinor = fatherMinorAf * motherMinorAf;
            het = fatherMajorAf * motherMinorAf + fatherMinorAf * motherMajorAf;
        } else if (chr.equals("X") && sex.equals("M")) {
            homMajor = motherMajorAf;
            homMinor = motherMinorAf;
            het = 1e-300;
        } else if (chr.equals("Y") && sex.equals("M")) {
            homMajor = fatherMajorAf;
            homMinor = fatherMinorAf;
            het = 1e-300;
        } else {
            return null;
        }

        double k = (1 - MOSAIC_RATE) / (homMajor + het + homMinor);

        homMajor *= k;
        het *= k;
        homMinor *= k;
        mosaic = MOSAIC_RATE;

        homMajor = log10(homMajor);
        het = log10(het);
        homMinor = log10(homMinor);
        mosaic = log10(mosaic);
        
        return new double[] {homMajor, het, homMinor, mosaic};
    }
    
    public double[] calcLikelihood(FilterEntry filterEntry, int majorId, int minorId) {
        return calcLikelihood(filterEntry, majorId, minorId, true);
    }
    public double[] calcLikelihood(FilterEntry filterEntry, int majorId, int minorId, boolean calcMosaic) {
        byte majorAllele = getBase(majorId);
        byte minorAllele = getBase(minorId);
        
        double[] p = new double[filterEntry.getDepth() + 1];
        p[0] = 1;
        int depth = 0;
        boolean useExp = false;
        for (int i = 0; i < filterEntry.getDepth(); ++i) {
            double q = Math.pow(10.0, filterEntry.getBaseQualities()[i] / -10.0);
            double qMajor, qMinor;
            if (filterEntry.getBases()[i] == majorAllele) {
                qMajor = 1 - q;
                qMinor = q;
            } else if (filterEntry.getBases()[i] == minorAllele) {
                qMajor = q;
                qMinor = 1 - q;
            } else {
                continue;
            }
            depth++;
            if (useExp) {
                qMajor = log10(qMajor);
                qMinor = log10(qMinor);
            }
            p[depth] = useExp ? p[depth - 1] + qMajor : p[depth - 1] * qMajor;
            if (calcMosaic) {
                for (int j = depth - 1; j > 0; --j) {
                    p[j] = useExp ? 
                            expAdd(p[j - 1] + qMajor, p[j] + qMinor) : 
                                p[j - 1] * qMajor + p[j] * (qMinor);
                }
            }
            p[0] = useExp ? p[0] + qMinor : p[0] * qMinor;
            
            if (!useExp && (p[0] < 1e300 || p[depth] < 1e300 || i == filterEntry.getDepth() - 1)) {
                for (int k = 0; k <= depth; ++k) {
                    p[k] = log10(p[k]);
                }
                useExp = true;
            }
        }
        
        double major = p[depth];
        double minor = p[0];
        double het = depth * log10(0.5);
        if (alphaParam > 0 && betaParam > 0) {
            het = -1e100;
            for (int r = 0; r <= depth; ++r) {
                het = expAdd(
                        het, 
                        p[r] + beta[alphaParam + betaParam + depth - 3][alphaParam + r - 1] 
                             - beta[alphaParam + betaParam - 3][alphaParam - 1]);
            }
        }
        
        double mosaic = -1e100;
        if (calcMosaic) {
            for (int r = 0; r <= depth; ++r) {
                mosaic = expAdd(mosaic, p[r] + beta[depth - 1][r]);
                //System.out.println(r + " " + format.format(Math.pow(10, p[r])));
            }
        }
        
        return new double[] { major, het, minor, mosaic };
    }
    
    private double[] calcPosterior(int majorId, int minorId, double[] p, double[] prior) {
        if (prior == null) {
            cnt[14]++;
            return null;
        }
       
        double homMajorPosterior = prior[0] + p[0];
        double hetPosterior = prior[1] + p[1];
        double homMinorPosterior = prior[2] + p[2];
        double mosaicPosterior = prior[3] + p[3] + 
                log10(baseChangeRate[majorId * 4 + minorId]);
        
        double sum = expAdd(homMajorPosterior, homMinorPosterior);
        sum = expAdd(sum, hetPosterior);
        sum = expAdd(sum, mosaicPosterior);   
        
        double finalHomMajor = homMajorPosterior - sum;
        double finalHet= hetPosterior - sum;
        double finalHomMinor = homMinorPosterior - sum;
        double finalMosaic = mosaicPosterior - sum;
                
        return new double[] {finalHomMajor, finalHet, finalHomMinor, finalMosaic};
    }
    
 

    public double[] getAF(FilterEntry filterEntry) {
        double[] af = null;
        if (dbSnpReader != null) {
            af = dbSnpReader.getAF(
                filterEntry.getChrName(), 
                filterEntry.getRefPos());
        }
        if (af == null) {
            cnt[4]++;
            int refId = getBaseId((char) filterEntry.getRef());
            if (refId < 0) {
                cnt[5]++;
                return null;
            }
            af = new double[] {DEFAULT_AF, DEFAULT_AF, DEFAULT_AF, DEFAULT_AF};
            af[refId] = 1;
        }
        return af;
    }
    
    private static double log10(double x) {
        if (x < 0) {
            x = 0;
        }
        return Math.log10(x);
    }
    private static double expAdd(double exp1, double exp2) {
        double diff = exp1 - exp2;
        if (diff < -12) {
            // exp1 is too small, return exp2
            return exp2;
        } else if (diff > 12) {
            // exp2 is too small, return exp1
            return exp1;
        } else {
            // lg(10^x + 10^y) = lg(10^x * (1+10^(y-x))) = x + lg(1+10^(y-x))
            return exp2 + log10(1.0 + Math.pow(10.0, diff));
        }
    }    
    
    private String formatAF(double af) {
        if (af == DEFAULT_AF) {
            return "N/A";
        } else if (af == UNKNOWN_AF) {
            return "UNKNOWN";
        } else {
            return String.valueOf(af);
        }
    }
    
    private int getBaseId(char base) {
        return "ACGT".indexOf(base);
    }
    
    private int getBaseId(String base) {
        return "ACGT".indexOf(base);
    }
    
    private byte getBase(int id) {
        return (byte) "ACGT".charAt(id);
    }
    
    private class DbSnpReader {
        private BufferedReader reader;
        private String currentLine = null;
        private String[] currentTokens = null;
        private String currentName = null;
        private int currentChrId = 0;
        private long currentPos = 0;        
        private boolean done = false;
        
        public DbSnpReader(BufferedReader reader) {
            this.reader = reader;
        }
        
        public double[] getAF(String chrName, long pos) {
            
            int chrId = MosaicHunterHelper.getChrId(chrName);
            if (chrId == 0) {
                return null;
            }
            
            while (currentChrId < chrId && !done) {
                nextLine();
            }

            if (done || currentChrId != chrId || pos < currentPos) {
                return null;
            }
            
            while (pos > currentPos && 
                   chrId == currentChrId && 
                   !done) {
                nextLine();
            }            
            if (!done && pos == currentPos && chrId == currentChrId) {
                return parseDbSnp(currentTokens);
            } 
            return null;
        }
                
        private void nextLine() {
            for (;;) {
                try {
                    currentLine = reader.readLine();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    done = true;
                    return;
                }
                if (currentLine == null) {
                    done = true;
                    return;
                }
                if (currentLine.isEmpty()) {
                    continue;
                }
                currentTokens = currentLine.split("\\t");
                if (!currentTokens[0].equals(currentName)) {
                    currentName = currentTokens[0];
                    currentChrId = MosaicHunterHelper.getChrId(currentName);
                    if (currentChrId == 0) {
                        done = true;
                        return;
                    }
                }                
                currentPos = Long.parseLong(currentTokens[1]); 
                break;

            }
        }
        
        private double[] parseDbSnp(String[] tokens) {
            if (tokens.length != 6) {
                return null;
            }
            if (tokens[3].length() != 1 || !"ACGT".contains(tokens[3])) {
                return null;
            }                
            double rate = 0;
            try {
                rate = Double.parseDouble(tokens[5]);
            } catch (Exception e) {
                return null;
            }
            
            double[] af = new double[] {DEFAULT_AF, DEFAULT_AF, DEFAULT_AF, DEFAULT_AF};                
            af[getBaseId(tokens[3])] = rate > 0 ? 1 - rate : 1;                
            String[] altTokens = tokens[4].split(",");
            int altCount = 0;
            for (String alt : altTokens) {
                if (alt.length() == 1 && "ACGT".contains(alt)) {
                    af[getBaseId(alt)] = rate > 0 ? rate : UNKNOWN_AF;
                    altCount++;
                }  
            }
            if (altCount == 0) {
                return null;
            }
            return af;
        }
        
    }

}
