package cn.edu.pku.cbi.mosaichunter.reference;

import java.io.Serializable;
import java.util.List;

public class Reference implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private final String metadata;
    private final long length;
    private final List<Sequence> sequences;
    
    public Reference(String name, String metadata, long length, List<Sequence> sequences) {
        this.name = name;
        this.metadata = metadata;
        this.length = length;
        this.sequences = sequences;
    }

    public String getName() {
        return name;
    }

    public String getMetadata() {
        return metadata;
    }

    public long getLength() {
        return length;
    }

    public List<Sequence> getSequences() {
        return sequences;
    }
    
}