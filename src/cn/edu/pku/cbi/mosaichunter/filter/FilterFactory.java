package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class FilterFactory {

    public static Filter create(String name) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("filter name is null.");
        }
        String className = ConfigManager.getInstance().get(name, "class");
        if (className == null || className.trim().isEmpty()) {
            throw new Exception(name + ".class property is missing");
        }
        
        try {
            Filter filter = (Filter) 
                    Class.forName(className).getConstructor(String.class).newInstance(name);
            return filter;
        } catch (Exception e) {
            throw new Exception("cannot create class " + className + "(" + name + ")", e);
        }
        
    }
    
    public static Filter[] create(String[] names) throws Exception {
        
        if (names == null) {
            return new Filter[0]; 
        }        
        Filter[] instances = new Filter[names.length];
        for (int i = 0; i < instances.length; ++i) {
            instances[i] = create(names[i]);
        }
        return instances;        
    }
    
}
