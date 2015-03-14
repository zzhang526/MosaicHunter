package cn.edu.pku.cbi.mosaichunter.config;

public class Parameter {

    private final String name;
    private final boolean optional;
    private final String defaultValue;
    private final String description;
    private final String[] details;

    public Parameter(
            String name, boolean optional, String defaultValue, 
            String description, String[] details) {
        this.name = name;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.description = description;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public String[] getDetails() {
        return details;
    }
    
}
