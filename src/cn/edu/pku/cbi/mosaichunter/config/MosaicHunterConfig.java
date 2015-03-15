package cn.edu.pku.cbi.mosaichunter.config;

public class MosaicHunterConfig {

    public static final String[] HELP_MESSAGES = new String[] {"help message"};
    
    public static final MosaicHunterConfig GENOME = new MosaicHunterConfig(
            "genome",
            "conf/genome.properties",
            new String[] {"mosaichunter genome help message, blah, blah, blah..."},
            new String[] {""},
            new Parameter[] {
                    Parameters.INPUT_FILE,
                    Parameters.REFERENCE_FILE,
                    Parameters.MODE,
                    Parameters.SEX,
                    Parameters.DBSNP_FILE,
                    Parameters.FATHER_BAM_FILE,
                    Parameters.MOTHER_BAM_FILE,
                    Parameters.CONTROL_BAM_FILE,
                    Parameters.REPETITIVE_REGION_BED_FILE,
                    Parameters.INDEL_REGION_BED_FILE,
                    Parameters.COMMON_SITE_BED_FILE,
                    Parameters.OUTPUT_DIR,
            });
        
    
    
    public static final MosaicHunterConfig EXOME = new MosaicHunterConfig(
            "exome",
            "conf/exome.properties",
            new String[] {"mosaichunter exome help message, blah, blah, blah..."},
            new String[] {""},
            new Parameter[] {
                    Parameters.INPUT_FILE,
                    Parameters.REFERENCE_FILE,
                    Parameters.MODE,
                    Parameters.ALPHA,
                    Parameters.BETA,
                    Parameters.SYSCALL_DEPTH,
                    Parameters.SEX,
                    Parameters.DBSNP_FILE,
                    Parameters.FATHER_BAM_FILE,
                    Parameters.MOTHER_BAM_FILE,
                    Parameters.CONTROL_BAM_FILE,
                    Parameters.REPETITIVE_REGION_BED_FILE,
                    Parameters.INDEL_REGION_BED_FILE,
                    Parameters.COMMON_SITE_BED_FILE,
                    Parameters.OUTPUT_DIR,
            });
    
    public static final MosaicHunterConfig EXOME_PARAMETERS = new MosaicHunterConfig(
            "exome_parameters",
            "conf/exome_parameters.properties",
            new String[] {"mosaichunter exome parameters help message, blah, blah, blah..."},
            null,
            new Parameter[] {
                    Parameters.INPUT_FILE,
                    Parameters.REFERENCE_FILE,
                    Parameters.SEX,
                    Parameters.DBSNP_FILE,
                    Parameters.REPETITIVE_REGION_BED_FILE,
                    Parameters.INDEL_REGION_BED_FILE,
                    Parameters.COMMON_SITE_BED_FILE,
                    Parameters.OUTPUT_DIR,       
            });
    
    
    
    private final String name;
    private final String configFile;
    private final String[] helpMessages;
    private final String[] shortHelpMessages;
    private final Parameter[] parameters;
    
    public MosaicHunterConfig(String name, String configFile,
            String[] helpMessages, String[] shortHelpMessages, Parameter[] parameters) {
        this.name = name;
        this.configFile = configFile;
        this.helpMessages = helpMessages;
        this.shortHelpMessages = shortHelpMessages;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getConfigFile() {
        return configFile;
    }

    public String[] getHelpMessages() {
        return helpMessages;
    }
    
    public String[] getShortHelpMessages() {
        return shortHelpMessages;
    }
    
    public Parameter[] getParameters() {
        return parameters;
    }
    
    public static MosaicHunterConfig getConfig(String name) {
        if (GENOME.name.equalsIgnoreCase(name)) {
            return GENOME;
        } else if (EXOME.name.equalsIgnoreCase(name)) {
            return EXOME;
        } else if (EXOME_PARAMETERS.name.equalsIgnoreCase(name)) {
            return EXOME_PARAMETERS;
        } else {
            return null;
        }
    }
    
}
