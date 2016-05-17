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

import java.io.File;

public class Validator {

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    public static boolean validateStringRequired(String name, String value, boolean required) {
        if (isEmpty(value)) {
            if (required) {
                System.out.println("Error - " + name + " is missing.");
                return false;
            } else {
                return true;
            }   
        } else {
            return true;
        }
    }
    
    public static boolean validateStringEnum(
            String name, String value, String[] validValues, boolean required) {
        if (!validateStringRequired(name, value, required)) {
            return false;
        }
        if (isEmpty(value)) {
            return true;
        }
        for (String s : validValues) {
            if (s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        System.out.println("Error - " + name + " is invalid: "+ value);
        return false;
    }
    
    public static boolean validateExists(String name, String value, boolean required) {
        if (!validateStringRequired(name, value, required)) {
            return false;
        }
        if (isEmpty(value)) {
            return true;
        }
        File f = new File(value);
        if (f.exists()) {
            return true;
        } else {
            System.out.println("Error - " + name + " does not exist: " + value);
            return false;
        }
    }
    
    public static boolean validateFileExists(String name, String value, boolean required) {
        if (!validateStringRequired(name, value, required)) {
            return false;
        }
        if (isEmpty(value)) {
            return true;
        }
        File f = new File(value);
        if (f.isFile()) {
            return true;
        } else {
            System.out.println("Error - " + name + " does not exist: " + value);
            return false;
        }
    }
    
    public static boolean validateCommandExists(String name, String value) {
        if (isEmpty(value)) {
            System.out.println("Error - " + name + " command is empty.");
        }
        
        Runtime rt = Runtime.getRuntime();
        Process blat;
        try {
            blat = rt.exec(value);
            Thread.sleep(2000);
            blat.destroy();
            blat.waitFor(); 
        } catch (Exception e) {
            System.out.println("Error - " + name + " command cannot be executed: " + value);
            return false;
        }
        
        return true;
    }
}
