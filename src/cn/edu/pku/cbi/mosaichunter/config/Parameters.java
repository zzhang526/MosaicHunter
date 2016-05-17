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

public class Parameters {
    
    public static final Parameter INPUT_FILE = new Parameter(
            null, "input_file", true, null, 
            "The input bam file.");
    
    public static final Parameter REFERENCE_FILE = new Parameter(
            null, "reference_file", true, null, 
            "The genome reference file. Should be consistent with input bam file.");
              
    public static final Parameter MODE = new Parameter(
            "mosaic_filter", "mode", false, "single", 
            "Could be 'single', 'trio', 'paired_naive', or 'paired_fisher'.");
    
    public static final Parameter SEX = new Parameter(
            "mosaic_filter", "sex", true, null, 
            "Sex.");
    
    public static final Parameter DBSNP_FILE = new Parameter(
            "mosaic_filter", "dbsnp_file", true, null, 
            "dbSNP file. Could download from our site.");
    
    public static final Parameter FATHER_BAM_FILE = new Parameter(
            "mosaic_filter", "father_bam_file", false, null, 
            "Father's bam file. Required for 'trio' mode.");
    
    public static final Parameter MOTHER_BAM_FILE = new Parameter(
            "mosaic_filter", "mother_bam_file", false, null, 
            "Mother's bam file. Required for 'trio' mode.");
    
    public static final Parameter CONTROL_BAM_FILE = new Parameter(
            "mosaic_filter", "control_bam_file", false, null, 
            "Control bam file. Required for 'paired_naive' or 'paired_fisher' mode.");
    
    public static final Parameter ALPHA = new Parameter(
            "mosaic_filter", "alpha_param", false, null, 
            "The alpha parameter.");
    
    public static final Parameter BETA = new Parameter(
            "mosaic_filter", "beta_param", false, null, 
            "The beta parameter.");
    
    public static final Parameter SYSCALL_DEPTH = new Parameter(
            "syscall_filter", "depth", false, 66, 
            "The average depth of input bam file.");
    
    public static final Parameter REPETITIVE_REGION_BED_FILE = new Parameter(
            "repetitive_region_filter", "bed_file", true, null, 
            "The .bed file of repetitive regions.");
    
    public static final Parameter INDEL_REGION_BED_FILE = new Parameter(
            "indel_region_filter", "bed_file", true, null, 
            "The .bed file of indel regions.");
    
    public static final Parameter COMMON_SITE_BED_FILE = new Parameter(
            "common_site_filter", "bed_file", true, null, 
            "The .bed file of commons sites.");
    
    public static final Parameter OUTPUT_DIR = new Parameter(
            null, "output_dir", false, "output", 
            "Where the output files go.");
    
}
