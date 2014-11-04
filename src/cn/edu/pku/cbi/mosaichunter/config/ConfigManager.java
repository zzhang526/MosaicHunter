package cn.edu.pku.cbi.mosaichunter.config;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class ConfigManager {

    private static final ConfigManager instance = new ConfigManager();
    
    private final Properties properties = new Properties();
    
    private ConfigManager() { 
    }
    
    public static ConfigManager getInstance() {
        return instance;
    }
    
    public void clear() {
        properties.clear();
    }
    
    public void set(String name, String value) {
        properties.setProperty(name, value);
    }
    
    public void set(String namespace, String name, String value) {
        properties.setProperty(namespace + "." + name, value);
    }
    
    public void putAll(Properties p) {
        properties.putAll(p);
    }
    public String get(String name) {
        return properties.getProperty(name);
    }
    
    public String get(String namespace, String name) {
        return get(namespace, name, null);
    }
    
    public String get(String namespace, String name, String defaultValue) {
        String key = namespace == null ? name : namespace + "." + name;
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
    
    
    public String[] getValues(String name) {
        return getValues(null, name);
    }
     
    public String[] getValues(String namespace, String name) {
        return getValues(namespace, name, null);
    }
    
    public String[] getValues(String namespace, String name, String[] defaultValues) {
        String value = get(namespace, name);
        if (value == null) {
            return defaultValues;
        }
        if (value.trim().isEmpty()) {
            return defaultValues;
        }
        return value.split(",");
    }
    
    public Boolean getBoolean(String name) {
        return getBoolean(null, name, null);
    }
    
    public Boolean getBoolean(String namespace, String name) {
        return getBoolean(namespace, name, null);
    }
    
    public Boolean getBoolean(String namespace, String name, Boolean defaultValue) {
        String value = get(namespace, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        value = value.trim().toLowerCase();
        return !(value.equals("false") || value.equals("0"));  
    }
    
    public Integer getInt(String namespace, String name) {
        return getInt(namespace, name, null);
    }
    
    public Integer getInt(String namespace, String name, Integer defaultValue) {
        String value = get(namespace, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
    
    public Long getLong(String namespace, String name) {
        return getLong(namespace, name, null);
    }
    
    public Long getLong(String namespace, String name, Long defaultValue) {
        String value = get(namespace, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }
    
    public void loadProperties(String configFile) throws IOException {
        FileReader reader = new FileReader(configFile);
        try {
            properties.load(reader);
        } finally {
            reader.close();
        }
    }
    
    public Double getDouble(String namespace, String name) {
        return getDouble(namespace, name, null);
    }
    
    public Double getDouble(String namespace, String name, Double defaultValue) {
        String value = get(namespace, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }
    
    public double[] getDoubles(String namespace, String name) {
        return getDoubles(namespace, name, null);
    }
    
    public double[] getDoubles(String namespace, String name, double[] defaultValue) {
        String[] values = getValues(namespace, name);
        if (values == null) {
            return defaultValue;
        }
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; ++i) {
            result[i] = Double.parseDouble(values[i]);
        }
        return result;
    }
    
    public void print() {
        Object[] keys = properties.keySet().toArray();
        Arrays.sort(keys);
        for (Object key : keys) {
            System.out.println(key + " = " + properties.get(key));
        }
    }
    
}
