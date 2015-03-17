package cn.edu.pku.cbi.mosaichunter.filter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import cn.edu.pku.cbi.mosaichunter.BamSiteReader;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterContext;
import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;
import cn.edu.pku.cbi.mosaichunter.Site;
import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.math.FishersExactTest;

public class MosaicFilter extends BaseFilter {

    public static final int DEFAULT_MAX_DEPTH = 500;
    
    // TODO must less than 1000
    public static final int DEFAULT_ALPHA_PARAM = -1;
    public static final int DEFAULT_BETA_PARAM = -1;
    
    public static final int DEFAULT_MIN_READ_QUALITY = 20;
    public static final int DEFAULT_MIN_MAPPING_QUALITY = 20;

    public static final String DEFAULT_SEX = "M";
    public static final double DEFAULT_MOSAIC_THRESHOLD = 0.05;
    public static final double DEFAULT_CONTROL_FISHER_THRESHOLD = 0.01;

    public static final int MAX_QUALITY = 64;
    public static final int MAX_QUALITY_DEPTH = 70;

    public static final double DEFAULT_DE_NOVO_RATE = 1e-8;
    public static final double DEFAULT_MOSAIC_RATE = 1e-7;
    public static final double DEFAULT_UNKNOWN_AF = 0.002;
    public static final double DEFAULT_NOVEL_AF = 1e-4;

    public static final double LOGZERO = -1e100;

    public static final double[] DEFAULT_BASE_CHANGE_RATE = new double[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }; // A, C, G, T

    private double[][] beta = null;
    private static double[][] c = null;

    private final int maxDepth;
    private final int alphaParam;
    private final int betaParam;
    private final int minReadQuality;
    private final int minMappingQuality;
    private final String mode;
    private final String sex;
    private final double[] baseChangeRate;
    private final double mosaicThreshold;
    private final String fatherBamFile;
    private final String fatherIndexFile;
    private final String motherBamFile;
    private final String motherIndexFile;

    private final String controlBamFile;
    private final String controlIndexFile;
    private final double controlFisherThreshold;
    
    public final double deNovoRate;
    public final double mosaicRate;
    public final double unknownAF;
    public final double novelAF;

    // TODO: New naive mode, added by Adam_Yyx, 2015-03-09 updated
    // public final int naiveMode = 8;
    public final double[] genotypeChangeRate_autosome = new double[16];
    public double[] log10_genotypeChangeRate_autosome = new double[16];
    public final double[] genotypeChangeRate_maleSexChr = new double[16];
    public double[] log10_genotypeChangeRate_maleSexChr = new double[16];
    public final boolean yyxDebug = false;

    private DbSnpReader dbSnpReader = null;
    private BamSiteReader fatherSiteReader = null;
    private BamSiteReader motherSiteReader = null;
    private BamSiteReader controlSiteReader = null;
    private final double[][] defaultAF = new double[4][4];

    public MosaicFilter(String name) {
        this(name, ConfigManager.getInstance().getInt(null, "max_depth", DEFAULT_MAX_DEPTH), ConfigManager.getInstance().getInt(null,
                "min_read_quality", DEFAULT_MIN_READ_QUALITY), ConfigManager.getInstance().getInt(null, "min_mapping_quality",
                DEFAULT_MIN_MAPPING_QUALITY), ConfigManager.getInstance().get(name, "mode"), ConfigManager.getInstance().getInt(name, "alpha_param", DEFAULT_ALPHA_PARAM), ConfigManager
                .getInstance().getInt(name, "beta_param", DEFAULT_BETA_PARAM), ConfigManager.getInstance().get(name, "sex", DEFAULT_SEX),
                ConfigManager.getInstance().getDoubles(name, "base_change_rate", DEFAULT_BASE_CHANGE_RATE), ConfigManager.getInstance()
                        .getDouble(name, "mosaic_threshold", DEFAULT_MOSAIC_THRESHOLD), ConfigManager.getInstance()
                        .get(name, "father_bam_file", null), ConfigManager.getInstance().get(name, "father_index_file", null),
                ConfigManager.getInstance().get(name, "mother_bam_file", null), ConfigManager.getInstance().get(name, "mother_index_file",
                        null), ConfigManager.getInstance().get(name,
                        "control_bam_file", null), ConfigManager.getInstance().get(name, "control_index_file", null), ConfigManager.getInstance().getDouble(name, "control_fisher_threshold",
                        DEFAULT_CONTROL_FISHER_THRESHOLD), ConfigManager.getInstance()
                        .getDouble(name, "de_novo_rate", DEFAULT_DE_NOVO_RATE), ConfigManager.getInstance().getDouble(name, "mosaic_rate",
                        DEFAULT_MOSAIC_RATE), ConfigManager.getInstance().getDouble(name, "unknown_af", DEFAULT_UNKNOWN_AF), ConfigManager
                        .getInstance().getDouble(name, "novel_af", DEFAULT_NOVEL_AF));
    }

    public MosaicFilter(String name, int maxDepth, int minReadQuality, int minMappingQuality, String mode, int alphaParam, int betaParam, String sex,
            double[] baseChangeRate, double mosaicThreshold, String fatherBamFile,
            String fatherIndexFile, String motherBamFile, String motherIndexFile, String controlBamFile,
            String controlIndexFile, double controlFisherThreshold,
            double deNovoRate, double mosaicRate, double unknownAF, double novelAF) {
        super(name);
        this.maxDepth = maxDepth;
        this.alphaParam = alphaParam;
        this.betaParam = betaParam;
        this.minReadQuality = minReadQuality;
        this.minMappingQuality = minMappingQuality;
        this.mode = mode == null ? "single" : mode.trim();
        this.sex = sex;
        this.baseChangeRate = baseChangeRate;
        this.mosaicThreshold = mosaicThreshold;
        this.fatherBamFile = fatherBamFile;
        this.fatherIndexFile = fatherIndexFile;
        this.motherBamFile = motherBamFile;
        this.motherIndexFile = motherIndexFile;
        this.controlBamFile = controlBamFile;
        this.controlIndexFile = controlIndexFile;
        this.controlFisherThreshold = controlFisherThreshold;
        this.deNovoRate = deNovoRate;
        this.mosaicRate = mosaicRate;
        this.unknownAF = unknownAF;
        this.novelAF = novelAF;

        // TODO: New naive mode, added by Adam_Yyx, 2015-03-09 updated

        // for autosome, female chrX
        // { 1 - 1e-8 - 1e-7, 1e-8, 0, 1e-7, // row: major-hom
        // 1e-8, 1 - 2e-8 - 1e-7, 1e-8, 1e-7, // row: het
        // 0, 1e-8, 1 - 1e-8 - 1e-7, 1e-7, // row: minor-hom
        // 1e-7, 1e-7, 1e-7, 1 - 3e-7 // row: mosaic
        // }; // major-hom, het, minor-hom, mosaic; from row to column

        // for male chrX, chrY
        // { 1 - 1e-8 - 1e-7, 0, 1e-8, 1e-7, // row: major-hom
        // 1e-8, 1 - 2e-8, 1e-8, 0, // row: het
        // 1e-8, 0, 1 - 1e-8 - 1e-7, 1e-7, // row: minor-hom
        // 1e-7, 0, 1e-7, 1 - 2e-7 // row: mosaic
        // }; // major-hom, het, minor-hom, mosaic; from row to column

        for (int i = 0; i < 4; i++) {
            genotypeChangeRate_autosome[i * 4 + i] = 1;
            genotypeChangeRate_maleSexChr[i * 4 + i] = 1;
            if (i < 3) { // from row = normal 3 genotype
                for (int j = 0; j < 3; j++) { // to column = normal 3 genotype
                    if (Math.abs(i - j) > 1) {
                        genotypeChangeRate_autosome[i * 4 + j] = LOGZERO;
                    } else if (i != j) {
                        genotypeChangeRate_autosome[i * 4 + j] = this.deNovoRate;
                        genotypeChangeRate_autosome[i * 4 + i] -= this.deNovoRate;
                    }
                    if (i == 1) { // from row = het
                        if (i != j) {
                            genotypeChangeRate_maleSexChr[i * 4 + j] = this.deNovoRate;
                            genotypeChangeRate_maleSexChr[i * 4 + i] -= this.deNovoRate;
                        }
                    } else { // from row = hom
                        if (j == 1) { // to column = het
                            genotypeChangeRate_maleSexChr[i * 4 + j] = LOGZERO;
                        } else if (i != j) {
                            genotypeChangeRate_maleSexChr[i * 4 + j] = this.deNovoRate;
                            genotypeChangeRate_maleSexChr[i * 4 + i] -= this.deNovoRate;
                        }
                    }
                }
                // to column = mosaic
                genotypeChangeRate_autosome[i * 4 + 3] = this.mosaicRate;
                genotypeChangeRate_autosome[i * 4 + i] -= this.mosaicRate;
                if (i != 1) { // from column != het, ie. = hom
                    genotypeChangeRate_maleSexChr[i * 4 + 3] = this.mosaicRate;
                    genotypeChangeRate_maleSexChr[i * 4 + i] -= this.mosaicRate;
                } else { // from column = het
                    genotypeChangeRate_maleSexChr[i * 4 + 3] = LOGZERO;
                }
            }
        }
        // from row = mosaic
        genotypeChangeRate_autosome[3 * 4 + 3] = 1;
        genotypeChangeRate_maleSexChr[3 * 4 + 3] = 1;
        for (int j = 0; j < 3; j++) { // to column = normal 3 genotype
            genotypeChangeRate_autosome[3 * 4 + j] = this.mosaicRate;
            genotypeChangeRate_autosome[3 * 4 + 3] -= this.mosaicRate;
            if (j != 1) { // to column != het, ie. = hom
                genotypeChangeRate_maleSexChr[3 * 4 + j] = this.mosaicRate;
                genotypeChangeRate_maleSexChr[3 * 4 + 3] -= this.mosaicRate;
            } else { // to column = het
                genotypeChangeRate_maleSexChr[3 * 4 + j] = LOGZERO;
            }
        }
        // TODO: for debug, by Adam_Yyx
        if (yyxDebug) {
            System.err.print("[DEBUG] genotypeChangeRate_autosome = {\n");
            for (int k = 0; k < 16; k++) {
                if (k % 4 == 0) {
                    System.err.print("  ");
                }
                System.err.print(genotypeChangeRate_autosome[k]);
                if (k < 15) {
                    System.err.print(", ");
                }
                if (k % 4 == 3) {
                    System.err.print("\n");
                }
            }
            System.err.print("};\n");

            System.err.print("[DEBUG] genotypeChangeRate_maleSexChr = {\n");
            for (int k = 0; k < 16; k++) {
                if (k % 4 == 0) {
                    System.err.print("  ");
                }
                System.err.print(genotypeChangeRate_maleSexChr[k]);
                if (k < 15) {
                    System.err.print(", ");
                }
                if (k % 4 == 3) {
                    System.err.print("\n");
                }
            }
            System.err.print("};\n");
        }

        for (int k = 0; k < 16; k++) {
            if (genotypeChangeRate_autosome[k] > 0) {
                log10_genotypeChangeRate_autosome[k] = log10(genotypeChangeRate_autosome[k]);
            } else {
                log10_genotypeChangeRate_autosome[k] = LOGZERO;
            }
            if (genotypeChangeRate_maleSexChr[k] > 0) {
                log10_genotypeChangeRate_maleSexChr[k] = log10(genotypeChangeRate_maleSexChr[k]);
            } else {
                log10_genotypeChangeRate_maleSexChr[k] = LOGZERO;
            }
        }
    }

    
    private boolean isTrio() {
        return "trio".equalsIgnoreCase(mode);
    }
    
    private boolean isHeterozygous() {
        return "heterozygous".equalsIgnoreCase(mode);
    }
    
    private boolean isPairedNaive() {
        return "paired_naive".equalsIgnoreCase(mode);
    }
    
    private boolean isPairedFisher() {
        return "paired_fisher".equalsIgnoreCase(mode);
    }
    
    @Override
    public void init(MosaicHunterContext context) throws Exception {
        super.init(context);
        if (beta == null) {
            initDbSnp();
            initTrio();
            initBeta();
        }
    }

    private void initTrio() throws Exception {
        if (isPairedFisher() || isPairedNaive()) {
            if (controlBamFile == null) {
                throw new Exception(getName() + ".control_bam_file property is missing");
            }
            controlSiteReader = new BamSiteReader(
                getContext().getReferenceManager(), controlBamFile, controlIndexFile, maxDepth, minReadQuality, minMappingQuality);
            controlSiteReader.init();
        } else if (isTrio()) {
            if (fatherBamFile == null) {
                throw new Exception(getName() + ".father_bam_file property is missing");
            }
            if (motherBamFile == null) {
                throw new Exception(getName() + ".mother_bam_file property is missing");
            }

            fatherSiteReader = new BamSiteReader(
                getContext().getReferenceManager(), fatherBamFile, fatherIndexFile, maxDepth, minReadQuality, minMappingQuality);
            fatherSiteReader.init();

            motherSiteReader = new BamSiteReader(
                getContext().getReferenceManager(), motherBamFile, motherIndexFile, maxDepth, minReadQuality, minMappingQuality);
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
                defaultAF[i][j] = i == j ? 1 : novelAF;
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
    public boolean doFilter(Site site) {

        if (site.getDepth() == 0) {
            return false;
        }

        boolean pass = false;
        if (isPairedNaive() || isPairedFisher()) {
            pass = calcMosaicWithControl(site);
        } else if (isHeterozygous()) {
            double het = calcHeterozygous(site);
            pass = het > mosaicThreshold;
        } else {
            double mosaic = isTrio() ? calcTrioMosaic(site) : calcIndividualMosaic(site);
            pass = mosaic > mosaicThreshold;
        }

        return pass;

    }

    private boolean calcMosaicWithControl(Site site) {
        Site control;
        try {
            control = controlSiteReader.read(site.getRefName(), site.getRefPos(), site.getRef(),
                    site.getAlleleCountOrder());
        } catch (Exception e) {
            cnt[201]++;
            return false;
        }
        if (control.getDepth() == 0) {
            cnt[207]++;
            return false;
        }

        double[] casePosterior = calcIndividualPosterior(site, false);
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
        int majorId = site.getMajorAlleleId();
        int minorId = site.getMinorAlleleId();
        alleleCounts[site.getMajorAlleleId()] = 1;
        if (casePosterior[0] < casePosterior[1] || casePosterior[0] < casePosterior[3]) {
            alleleCounts[site.getMinorAlleleId()] = 1;
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

        byte majorAllele = MosaicHunterHelper.ID_TO_BASE[majorId];
        byte minorAllele = MosaicHunterHelper.ID_TO_BASE[minorId];

        int a = site.getAlleleCount(majorAllele);
        int b = site.getAlleleCount(minorAllele);

        /**************/
        // System.out.println(majorId + " " + (char) getBase(majorId) + " " + control.getAlleleCount(majorAllele));
        // System.out.println(minorId + " " + (char) getBase(minorId) + " " + control.getAlleleCount(minorAllele));
        // System.out.println(control.getAlleleCount()+" "+control.getAlleleCount(1)+" "+control.getAlleleCount(2)+" "+control.getAlleleCountById(3));
        // System.out.println(control.getAlleleCountOrder());
        // System.out.println(site.getAlleleCount(0)+" "+site.getAlleleCount(1)+" "+
        // site.getAlleleCount(2)+" "+site.getAlleleCount(3));
        // System.out.println(site.getAlleleCountOrder());

        int c = control.getAlleleCount(majorAllele);
        int d = control.getAlleleCount(minorAllele);

        if (isPairedFisher()) {
            double p = FishersExactTest.twoSided(a, b, c, d);
            site.setMetadata(getName(), new Object[] {
                    "Case(" + (char) getBase(majorId) + ":" + a + "," + (char) getBase(minorId) + ":" + b + ")",
                    "Control(" + (char) getBase(majorId) + ":" + c + "," + (char) getBase(minorId) + ":" + d + ")", casePosterior[0],
                    casePosterior[1], casePosterior[2], casePosterior[3], controlPosterior[0], controlPosterior[1], controlPosterior[2],
                    controlPosterior[3], p });
            return p < controlFisherThreshold;
        }

        // TODO: New naive mode, added by Adam_Yyx, 2015-03-09 updated
        double[] af = getAF(site);
        if (af == null) {
            return false;
        }

        double[] prior = calcPrior(af, af, site, sex, majorId, minorId);
        if (prior == null)
            return false;
        double[] caseLikelihood = calcLikelihood(site, majorId, minorId);
        double[] controlLikelihood = calcLikelihood(control, majorId, minorId);

        // PGM for naiveMode 1-4,
        // population prior -> control -> case
        // P(case_genotype|control_genotype) follow genotypeChangeRate[1:16]
        // 1,3: only focus on mosaic; 2,4: sum all difference
        // 1,2: no prior; 3,4: use prior
        // PGM for naiveMode 5-8:
        // population prior -> ancestor (unobserved), ancestor -> case, ancestor -> control
        // P(case_genotype|ancestor_genotype) and P(control_genotype|ancestor_genotype) follow genotypeChangeRate[1:16]
        // 5,7: only focus on mosaic; 6,8: sum all difference
        // 5,6: no prior; 7,8: use prior

        // only retain naiveMode 8
        // if (naiveMode >= 5 && naiveMode <= 8) {
        boolean isAutosome = true;
        String chr = site.getRefName();
        if ((!chr.equals("X") && !chr.equals("Y")) || (chr.equals("X") && sex.equals("F"))) {
            isAutosome = true;
        } else if (chr.equals("X") && sex.equals("M")) {
            isAutosome = false;
        } else if (chr.equals("Y") && sex.equals("M")) {
            isAutosome = false;
        } else {
            return false;
        }
        double[] ancestorCaseControlJointPosterior = new double[4 * 4 * 4];
        double sumPosteriorNormalizationFactor = LOGZERO;
        for (int n = 0; n < 4; n++) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (isAutosome) {
                        ancestorCaseControlJointPosterior[n * 4 * 4 + i * 4 + j] = caseLikelihood[i] + controlLikelihood[j]
                                + log10_genotypeChangeRate_autosome[n * 4 + i] + log10_genotypeChangeRate_autosome[n * 4 + j];
                    } else {
                        ancestorCaseControlJointPosterior[n * 4 * 4 + i * 4 + j] = caseLikelihood[i] + controlLikelihood[j]
                                + log10_genotypeChangeRate_maleSexChr[n * 4 + i] + log10_genotypeChangeRate_maleSexChr[n * 4 + j];
                    }
                    // if (((naiveMode - 1) & 0x2) > 0) { // 7,8
                    ancestorCaseControlJointPosterior[n * 4 * 4 + i * 4 + j] += prior[n];
                    // }
                    sumPosteriorNormalizationFactor = expAdd(sumPosteriorNormalizationFactor, ancestorCaseControlJointPosterior[n * 4 * 4
                            + i * 4 + j]);
                }
            }
        }
        double[] caseControlJointPosterior = new double[4 * 4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                caseControlJointPosterior[i * 4 + j] = LOGZERO;
                for (int n = 0; n < 4; n++) {
                    caseControlJointPosterior[i * 4 + j] = expAdd(caseControlJointPosterior[i * 4 + j], ancestorCaseControlJointPosterior[n
                            * 4 * 4 + i * 4 + j]
                            - sumPosteriorNormalizationFactor);
                }
            }
        }

        double[] caseMarginalPosterior = new double[4];
        double[] controlMarginalPosterior = new double[4];
        for (int k = 0; k < 4; k++) {
            caseMarginalPosterior[k] = LOGZERO;
            controlMarginalPosterior[k] = LOGZERO;
        }
        double sumLog10Posterior = LOGZERO;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                caseMarginalPosterior[i] = expAdd(caseMarginalPosterior[i], caseControlJointPosterior[i * 4 + j]);
                controlMarginalPosterior[j] = expAdd(controlMarginalPosterior[j], caseControlJointPosterior[i * 4 + j]);
                if (i == j)
                    continue;
                sumLog10Posterior = expAdd(sumLog10Posterior, caseControlJointPosterior[i * 4 + j]);
            }
        }
        site.setMetadata(getName(), new Object[] {
                "Case(" + (char) getBase(majorId) + ":" + a + "," + (char) getBase(minorId) + ":" + b + ")",
                "Control(" + (char) getBase(majorId) + ":" + c + "," + (char) getBase(minorId) + ":" + d + ")", caseMarginalPosterior[0],
                caseMarginalPosterior[1], caseMarginalPosterior[2], caseMarginalPosterior[3], controlMarginalPosterior[0],
                controlMarginalPosterior[1], controlMarginalPosterior[2], controlMarginalPosterior[3], caseLikelihood[0],
                caseLikelihood[1], caseLikelihood[2], caseLikelihood[3], controlLikelihood[0], controlLikelihood[1], controlLikelihood[2],
                controlLikelihood[3], caseControlJointPosterior[0], caseControlJointPosterior[1], caseControlJointPosterior[2],
                caseControlJointPosterior[3], caseControlJointPosterior[4], caseControlJointPosterior[5], caseControlJointPosterior[6],
                caseControlJointPosterior[7], caseControlJointPosterior[8], caseControlJointPosterior[9], caseControlJointPosterior[10],
                caseControlJointPosterior[11], caseControlJointPosterior[12], caseControlJointPosterior[13], caseControlJointPosterior[14],
                caseControlJointPosterior[15], sumLog10Posterior });
        return sumLog10Posterior > log10(mosaicThreshold);

    }

    private double calcTrioMosaic(Site child) {
        Site father;
        try {
            father = fatherSiteReader.read(child.getRefName(), child.getRefPos(), child.getRef(), child.getAlleleCountOrder());
        } catch (Exception e) {
            cnt[101]++;
            return -1;
        }

        Site mother;
        try {

            mother = motherSiteReader.read(child.getRefName(), child.getRefPos(), child.getRef(), child.getAlleleCountOrder());

        } catch (Exception e) {
            cnt[102]++;
            return -1;
        }

        // System.out.println(this.buildOutput(father));
        // System.out.println(this.buildOutput(mother));

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

        double[] fatherPrior = calcPrior(af, af, father, "M", father.getMajorAlleleId(), father.getMinorAlleleId());
        double[] motherPrior = calcPrior(af, af, mother, "F", mother.getMajorAlleleId(), mother.getMinorAlleleId());

        if (motherPrior == null) {
            cnt[106]++;
            motherPrior = new double[] { 0, 0, 0, 0 };
        }
        print10(fatherPrior, "father prior");
        print10(motherPrior, "mother prior");

        double[] fatherLikelihood = calcLikelihood(father, father.getMajorAlleleId(), father.getMinorAlleleId());
        double[] motherLikelihood = calcLikelihood(mother, mother.getMajorAlleleId(), mother.getMinorAlleleId());
        double[] childLikelihood = calcLikelihood(child, child.getMajorAlleleId(), child.getMinorAlleleId());

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
        double[] fatherPosterior = new double[] { LOGZERO, LOGZERO, LOGZERO, LOGZERO };
        double[] motherPosterior = new double[] { LOGZERO, LOGZERO, LOGZERO, LOGZERO };
        double[] childPosterior = new double[] { LOGZERO, LOGZERO, LOGZERO, LOGZERO };
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                for (int k = 0; k < 4; ++k) {
                    joint[i][j][k] = fatherLikelihood[i] + motherLikelihood[j] + childLikelihood[k] + fatherPrior[i] + motherPrior[j]
                            + cpd[i][j][k];
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
        // System.out.println(sum);
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
                        (char) child.getMajorAllele() + "(" + formatAF(af[child.getMajorAlleleId()]) + "):" + (char) child.getMinorAllele()
                                + "(" + formatAF(af[child.getMinorAlleleId()]) + ")",
                        "F(" + (char) father.getMajorAllele() + ":" + father.getMajorAlleleCount() + "," + (char) father.getMinorAllele()
                                + ":" + father.getMinorAlleleCount() + ")",
                        "M(" + (char) mother.getMajorAllele() + ":" + mother.getMajorAlleleCount() + "," + (char) mother.getMinorAllele()
                                + ":" + mother.getMinorAlleleCount() + ")",

                        childLikelihood[0], childLikelihood[1], childLikelihood[2], childLikelihood[3], fatherPosterior[0],
                        fatherPosterior[1], fatherPosterior[2], fatherPosterior[3], motherPosterior[0], motherPosterior[1],
                        motherPosterior[2], motherPosterior[3], childPosterior[0], childPosterior[1], childPosterior[2], childPosterior[3],
                        mosaic, childLikelihood[3] - childLikelihood[1]});
        return mosaic;
    }

    private double[][] calcBaseProb(Site individual) {
        double[][] prob = new double[4][];
        for (int genotype = 0; genotype < 4; ++genotype) {
            double[] p = new double[] { deNovoRate / 3, deNovoRate / 3, deNovoRate / 3, deNovoRate / 3 };
            if (genotype == 0) {
                // hom ref
                p[individual.getMajorAlleleId()] = 1.0 - deNovoRate;
            } else if (genotype == 1) {
                // het
                p[individual.getMajorAlleleId()] = 0.5 - deNovoRate / 3;
                p[individual.getMinorAlleleId()] = 0.5 - deNovoRate / 3;
            } else if (genotype == 2) {
                // hom alt
                p[individual.getMinorAlleleId()] = 1.0 - deNovoRate;
            } else {
                // mosaic
                double mosaicAF = (double) individual.getMinorAlleleCount()
                        / (individual.getMajorAlleleCount() + individual.getMinorAlleleCount());
                // TODO
                if (individual.getMinorAlleleCount() == 0) {
                    mosaicAF = mosaicRate;
                }
                p[individual.getMajorAlleleId()] = (1 - mosaicAF) * (1 - deNovoRate / 3 * 2);
                p[individual.getMinorAlleleId()] = mosaicAF * (1 - deNovoRate / 3 * 2);
            }
            prob[genotype] = p;
        }
        return prob;
    }

    private double[][][] calcCPD(Site father, Site mother, Site child) {
        double[][] fatherBaseProb = calcBaseProb(father);
        double[][] motherBaseProb = calcBaseProb(mother);

        double[][][] cpd = new double[4][4][4];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                cpd[i][j] = calcChildBaseProb(fatherBaseProb[i][child.getMajorAlleleId()], fatherBaseProb[i][child.getMinorAlleleId()],
                        motherBaseProb[j][child.getMajorAlleleId()], motherBaseProb[j][child.getMinorAlleleId()], child.getRefName(), sex);
                if (cpd[i][j] == null) {
                    return null;
                }
                print10(cpd[i][j], "cpd " + i + " " + j);

            }
        }
        return cpd;
    }

    private double calcIndividualMosaic(Site site) {
        double[] posterior = calcIndividualPosterior(site, true);
        if (posterior == null) {
            return -1;
        }
        double mosaic = Math.pow(10, posterior[3]);
        return mosaic;
    }

    private double calcHeterozygous(Site site) {

        int majorId = site.getMajorAlleleId();
        int minorId = site.getMinorAlleleId();

        double[] af = getAF(site);
        if (af == null) {
            return 0;
        }

        double[] prior = calcPrior(af, af, site, sex, majorId, minorId);
        double[] likelihood = calcLikelihood(site, majorId, minorId, false);
        double[] posterior = calcPosterior(majorId, minorId, likelihood, prior);
        if (posterior == null) {
            return 0;
        }

        site.setMetadata(getName(),
                new Object[] {
                        (char) getBase(majorId) + "(" + formatAF(af[majorId]) + "):" + (char) getBase(minorId) + "("
                                + formatAF(af[minorId]) + ")", prior[0], prior[1], prior[2], likelihood[0], likelihood[1], likelihood[2],
                        posterior[0], posterior[1], posterior[2], Math.pow(10, posterior[1]) });

        double het = Math.pow(10, posterior[1]);
        return het;
    }

    private double[] calcIndividualPosterior(Site site, boolean setMetadata) {
        byte majorAllele = site.getMajorAllele();
        byte minorAllele = site.getMinorAllele();
        int majorId = getBaseId((char) majorAllele);
        int minorId = getBaseId((char) minorAllele);
        return calcIndividualPosterior(site, setMetadata, majorId, minorId);
    }

    private double[] calcIndividualPosterior(Site site, boolean setMetadata, int majorId, int minorId) {

        double[] af = getAF(site);
        if (af == null) {
            return null;
        }

        double[] prior = calcPrior(af, af, site, sex, majorId, minorId);
        double[] likelihood = calcLikelihood(site, majorId, minorId);
        double[] posterior = calcPosterior(majorId, minorId, likelihood, prior);
        if (posterior == null) {
            return null;
        }

        if (setMetadata) {
            site.setMetadata(getName(), new Object[] {
                    (char) getBase(majorId) + "(" + formatAF(af[majorId]) + "):" + (char) getBase(minorId) + "(" + formatAF(af[minorId])
                            + ")", prior[0], prior[1], prior[2], prior[3], likelihood[0], likelihood[1], likelihood[2], likelihood[3],
                    posterior[0], posterior[1], posterior[2], posterior[3], Math.pow(10, posterior[3]) });
        }
        return posterior;
    }

    private void print(double[] d, String name) {
        if (true)
            return;
        if (d != null)
            System.out.println(name + "\t" + format.format(Math.pow(10, d[0])) + "\t" + format.format(Math.pow(10, d[1])) + "\t"
                    + format.format(Math.pow(10, d[2])) + "\t" + format.format(Math.pow(10, d[3])) + "\t");

    }

    private void print10(double[] d, String name) {

        if (true)
            return;
        if (d != null)
            System.out.println(name + "\t" + format.format(d[0]) + "\t" + format.format(d[1]) + "\t" + format.format(d[2]) + "\t"
                    + format.format(d[3]) + "\t");

    }

    private double[] calcPrior(double[] fatherAf, double[] motherAf, Site site, String sex, int majorAlleleId, int minorAlleleId) {
        double fatherMajorAf = fatherAf[majorAlleleId] / (fatherAf[majorAlleleId] + fatherAf[minorAlleleId]);
        double motherMajorAf = motherAf[majorAlleleId] / (motherAf[majorAlleleId] + motherAf[minorAlleleId]);
        return calcChildBaseProb(fatherMajorAf, 1 - fatherMajorAf, motherMajorAf, 1 - motherMajorAf, site.getRefName(), sex);
    }

    private double[] calcChildBaseProb(double fatherMajorAf, double fatherMinorAf, double motherMajorAf, double motherMinorAf, String chr,
            String sex) {

        double homMajor = 0;
        double het = 0;
        double homMinor = 0;
        double mosaic = 0;
        if ((!MosaicHunterHelper.isChrX(chr) && !MosaicHunterHelper.isChrY(chr)) || (MosaicHunterHelper.isChrX(chr) && sex.equals("F"))) {
            homMajor = fatherMajorAf * motherMajorAf;
            homMinor = fatherMinorAf * motherMinorAf;
            het = fatherMajorAf * motherMinorAf + fatherMinorAf * motherMajorAf;
        } else if (MosaicHunterHelper.isChrX(chr) && sex.equals("M")) {
            homMajor = motherMajorAf;
            homMinor = motherMinorAf;
            het = 1e-300;
        } else if (MosaicHunterHelper.isChrY(chr) && sex.equals("M")) {
            homMajor = fatherMajorAf;
            homMinor = fatherMinorAf;
            het = 1e-300;
        } else {
            return null;
        }

        double k = (1 - mosaicRate) / (homMajor + het + homMinor);

        homMajor *= k;
        het *= k;
        homMinor *= k;
        mosaic = mosaicRate;

        homMajor = log10(homMajor);
        het = log10(het);
        homMinor = log10(homMinor);
        mosaic = log10(mosaic);

        return new double[] { homMajor, het, homMinor, mosaic };
    }

    public double[] calcLikelihood(Site site, int majorId, int minorId) {
        return calcLikelihood(site, majorId, minorId, true);
    }

    public double[] calcLikelihood(Site site, int majorId, int minorId, boolean calcMosaic) {
        byte majorAllele = getBase(majorId);
        byte minorAllele = getBase(minorId);

        double[] p = new double[site.getDepth() + 1];
        p[0] = 1;
        int depth = 0;
        boolean useExp = false;
        for (int i = 0; i < site.getDepth(); ++i) {
            double q = Math.pow(10.0, site.getBaseQualities()[i] / -10.0);
            double qMajor, qMinor;
            if (site.getBases()[i] == majorAllele) {
                qMajor = 1 - q;
                qMinor = q;
            } else if (site.getBases()[i] == minorAllele) {
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
                    p[j] = useExp ? expAdd(p[j - 1] + qMajor, p[j] + qMinor) : p[j - 1] * qMajor + p[j] * (qMinor);
                }
            }
            p[0] = useExp ? p[0] + qMinor : p[0] * qMinor;

            if (!useExp && (p[0] < 1e300 || p[depth] < 1e300 || i == site.getDepth() - 1)) {
                for (int k = 0; k <= depth; ++k) {
                    p[k] = log10(p[k]);
                }
                useExp = true;
            }
        }

        double major = p[depth];
        double minor = p[0];
        double het = depth * log10(0.5);
        // TODO what about 1 and 1, print an error message?
        if (alphaParam > 0 && betaParam > 0 && alphaParam + betaParam > 2) {
            het = LOGZERO;
            for (int r = 0; r <= depth; ++r) {
                het = expAdd(het, p[r] + beta[alphaParam + betaParam + depth - 3][alphaParam + r - 1]
                        - beta[alphaParam + betaParam - 3][alphaParam - 1]);
            }
        }

        double mosaic = LOGZERO;
        if (calcMosaic) {
            for (int r = 0; r <= depth; ++r) {
                mosaic = expAdd(mosaic, p[r] + beta[depth - 1][r]);
                // System.out.println(r + " " + format.format(Math.pow(10, p[r])));
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
        double mosaicPosterior = prior[3] + p[3] + log10(baseChangeRate[majorId * 4 + minorId]);

        double sum = expAdd(homMajorPosterior, homMinorPosterior);
        sum = expAdd(sum, hetPosterior);
        sum = expAdd(sum, mosaicPosterior);

        double finalHomMajor = homMajorPosterior - sum;
        double finalHet = hetPosterior - sum;
        double finalHomMinor = homMinorPosterior - sum;
        double finalMosaic = mosaicPosterior - sum;

        return new double[] { finalHomMajor, finalHet, finalHomMinor, finalMosaic };
    }

    public double[] getAF(Site site) {
        double[] af = null;
        if (dbSnpReader != null) {
            af = dbSnpReader.getAF(site.getRefName(), site.getRefPos());
        }
        if (af == null) {
            cnt[4]++;
            int refId = getBaseId((char) site.getRef());
            if (refId < 0) {
                cnt[5]++;
                return null;
            }
            af = new double[] { novelAF, novelAF, novelAF, novelAF };
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
        if (af == novelAF) {
            return "N/A";
        } else if (af == unknownAF) {
            return "UNKNOWN";
        } else {
            return String.valueOf(af);
        }
    }

    private int getBaseId(char base) {
        return MosaicHunterHelper.BASE_TO_ID[base];
    }

    private byte getBase(int id) {
        return MosaicHunterHelper.ID_TO_BASE[id];
    }

    private class DbSnpReader {
        private BufferedReader reader;
        private String currentLine = null;
        private String[] currentTokens = null;
        private String currentName = null;
        private int currentChrId = -1;
        private long currentPos = 0;
        private boolean done = false;

        public DbSnpReader(BufferedReader reader) {
            this.reader = reader;
        }

        public double[] getAF(String chrName, long pos) {

            int chrId = getContext().getReferenceManager().getReferenceId(chrName);
            if (chrId < 0) {
                return null;
            }

            while (currentChrId < chrId && !done) {
                nextLine();
            }

            if (done || currentChrId != chrId || pos < currentPos) {
                return null;
            }

            while (pos > currentPos && chrId == currentChrId && !done) {
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
                    currentChrId = getContext().getReferenceManager().getReferenceId(currentName);
                    if (currentChrId < 0) {
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

            double[] af = new double[] { novelAF, novelAF, novelAF, novelAF };
            af[getBaseId(tokens[3].charAt(0))] = rate > 0 ? 1 - rate : 1;
            String[] altTokens = tokens[4].split(",");
            int altCount = 0;
            for (String alt : altTokens) {
                if (alt.length() == 1 && "ACGT".contains(alt)) {
                    af[getBaseId(alt.charAt(0))] = rate > 0 ? rate : unknownAF;
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
