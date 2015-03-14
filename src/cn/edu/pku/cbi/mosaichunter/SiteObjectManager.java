package cn.edu.pku.cbi.mosaichunter;

public class SiteObjectManager {

    private final int[] lengths;
    private final SiteFactory[] siteFactories;
    
    public SiteObjectManager(int minLength, int maxLength) {
        int n = 1;
        int l = minLength;
        while (l < maxLength) {
            n++;
            l *= 2;
        }
        lengths = new int[n];
        siteFactories = new SiteFactory[n];
        
        lengths[0] = minLength;
        for (int i = 1; i < n - 1; ++i) {
            lengths[i] = lengths[i - 1] * 2;
        }
        lengths[n - 1] = maxLength;
        
        for (int i = 0; i < n; ++i) {
            siteFactories[i] = new SiteFactory(lengths[i]);
        }
    }
    
    public void printInfo() {
        for (int i = 0; i < lengths.length; ++i) {
            System.out.println(i + " " + siteFactories[i].getName() + " " + siteFactories[i].getSize());
        }
        
        for (int i = 0; i < cnt.length; ++i) {
            if (cnt[i] > 0) {
                System.out.println(i + " " + cnt[i]);
            }
        }
    }
    static long[] cnt = new long[100];
    public Site getSite(int length) {
        if (length <= lengths[0]) {
            return siteFactories[0].getObject();
        }
        for (int i = 1; i < lengths.length; ++i) {
            if (length <= lengths[i]) {
                return siteFactories[i].getObject();
            }
        }
        StatsManager.count("site_object_manager.get.invalid_length");
        return null;
    }
    
    public void returnSite(Site site) {
        if (site == null) {
            return;
        }
        if (site.getMaxDepth()  <= lengths[0]) {
            siteFactories[0].returnObject(site);
            return;
        }
        for (int i = 1; i < lengths.length; ++i) {
            if (site.getMaxDepth() == lengths[i]) {
                siteFactories[i].returnObject(site);
                return;
            }
        }
        StatsManager.count("site_object_manager.return.invalid_length");
    } 
    

    private class SiteFactory extends ObjectFactory<Site> {
    
        private final int length;
        
        public SiteFactory(int length) {
            super("Site[" + length + "]");
            this.length = length;
        }
        
        @Override
        public Site createObject() {
            return new Site(length);
        }
    }
}
