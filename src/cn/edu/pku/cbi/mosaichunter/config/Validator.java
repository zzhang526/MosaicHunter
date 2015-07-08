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
