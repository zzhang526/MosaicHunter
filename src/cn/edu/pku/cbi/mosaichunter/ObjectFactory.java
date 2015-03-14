package cn.edu.pku.cbi.mosaichunter;

import java.util.LinkedList;

abstract public class ObjectFactory<T> {

    private final LinkedList<T> objects = new LinkedList<T>();
    private final String name;
    
    public ObjectFactory(String name) {
        this.name = name;
    }
    
    abstract public T createObject(); 
   
    public T getObject() {
        if (objects.isEmpty()) {
            StatsManager.count("object_factory.new");
            return createObject();
        } else {
            StatsManager.count("object_factory.reuse");
            return objects.pop();
        }
    }
    
    public void returnObject(T object) {
        StatsManager.count("object_factory.return");
        objects.addFirst(object);
    }
    
    public String getName() {
        return name;
    }
    
    public int getSize() {
        return objects.size();
    }
    
}
