package cn.edu.pku.cbi.mosaichunter.config;

public class MosaicHunterConfig {

    public static final String[] HELP_MESSAGES = new String[] {"help message"};
    
    public static final MosaicHunterConfig GENOME = new MosaicHunterConfig(
            "genome",
            "conf/genome.properties",
            new String[] {"mosaichunter genome help message"},
            new String[] {"mosaichunter genome short help message"},
            new Parameter[] {
                    Parameters.INPUT_FILE,
                    Parameters.INPUT_FILE
            });
    
    public static final MosaicHunterConfig EXOME = new MosaicHunterConfig(
            "exome",
            "conf/exome.properties",
            new String[] {"mosaichunter exome help message"},
            new String[] {"mosaichunter exome short help message"},
            new Parameter[] {
                    
            });
    
    public static final MosaicHunterConfig EXOME_PARAMETER = new MosaicHunterConfig(
            "exome_parameter",
            "conf/exome_perameters.properties",
            new String[] {"mosaichunter exome parameter help message"},
            new String[] {"mosaichunter exome parameter short help message"},
            new Parameter[] {
                    
            });
    
    public static final MosaicHunterConfig CANCER = new MosaicHunterConfig(
            "cancer",
            "conf/cancer.properties",
            new String[] {"mosaichunter cancer help message"},
            new String[] {"mosaichunter cancer short help message"},
            new Parameter[] {
                    
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
        } else if (EXOME_PARAMETER.name.equalsIgnoreCase(name)) {
            return EXOME_PARAMETER;
        }else if (CANCER.name.equalsIgnoreCase(name)) {
            return CANCER;
        } else {
            return null;
        }
    }
    
}
