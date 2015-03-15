package cn.edu.pku.cbi.mosaichunter.config;

public class Parameter {

    private final String namespace;
    private final String name;
    private final boolean required;
    private final Object defaultValue;
    private final String description;
    private final String[] details;

    public Parameter(
            String namesapce, String name, boolean optional, Object defaultValue, 
            String description) {
        this(namesapce, name, optional, defaultValue, description, null);
    }
    
    public Parameter(
            String namesapce, String name, boolean required, Object defaultValue, 
            String description, String[] details) {
        this.namespace = namesapce;
        this.name = name;
        this.required = required;
        this.defaultValue = defaultValue;
        this.description = description;
        this.details = details;
    }

    public String getNamespace() {
        return namespace;
    }
    
    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public String[] getDetails() {
        return details;
    }
    
}
