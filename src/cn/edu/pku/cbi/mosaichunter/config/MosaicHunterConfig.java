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

package cn.edu.pku.cbi.mosaichunter.config;

public class MosaicHunterConfig {

    public static final String[] HELP_MESSAGES = new String[] {"help message"};
    
    public static final MosaicHunterConfig GENOME = new MosaicHunterConfig(
            "genome",
            "genome.properties",
            new String[] {"This mode is for genome data."},
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
            "exome.properties",
            new String[] {"This mode is for exome data."},
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
            "exome_parameters.properties",
            new String[] {"Calculates parameters for exome data."},
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
