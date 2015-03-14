package cn.edu.pku.cbi.mosaichunter;

import cn.edu.pku.cbi.mosaichunter.reference.ReferenceManager;
import net.sf.samtools.SAMFileReader;

public class MosaicHunterContext {

    private final SAMFileReader samFileReader;
    private final ReferenceManager referenceManager;
    private final ReadsCache readsCache;

    
    public MosaicHunterContext(
            SAMFileReader samFileReader, 
            ReferenceManager referenceManager, 
            ReadsCache readsCache) {
        this.samFileReader = samFileReader;
        this.referenceManager = referenceManager;
        this.readsCache = readsCache;
    }
    
    public SAMFileReader getSAMFileReader() {
        return samFileReader;
    }
    
    public ReferenceManager getReferenceManager() {
        return referenceManager;
    }
    
    public ReadsCache getReadsCache() {
        return readsCache;
    }
    
}
