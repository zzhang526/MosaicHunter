package cn.edu.pku.cbi.mosaichunter;

import net.sf.samtools.SAMRecord;

public class ArrayManager {

    private final int[] lengths;
    private final ShortArrayFactory[] shortArrayFactories;
    private final ByteArrayFactory[] byteArrayFactories;
    private final SAMRecordArrayFactory[] samRecordArrayFactories;

    public ArrayManager(int minLength, int maxLength) {
        int n = 1;
        int l = minLength;
        while (l < maxLength) {
            n++;
            l *= 2;
        }
        lengths = new int[n];
        shortArrayFactories = new ShortArrayFactory[n];
        byteArrayFactories = new ByteArrayFactory[n];
        samRecordArrayFactories = new SAMRecordArrayFactory[n];
        
        lengths[0] = minLength;
        for (int i = 1; i < n - 1; ++i) {
            lengths[i] = lengths[i - 1] * 2;
        }
        lengths[n - 1] = maxLength;
        
        for (int i = 0; i < n; ++i) {
            shortArrayFactories[i] = new ShortArrayFactory(lengths[i]);
            byteArrayFactories[i] = new ByteArrayFactory(lengths[i]);
            samRecordArrayFactories[i] = new SAMRecordArrayFactory(lengths[i]);
        }
    }
    
    private<T> T get(int length, ObjectFactory<T>[] ofs) {
        for (int i = 0; i < lengths.length; ++i) {
            if (length <= lengths[i]) {
                return ofs[i].getObject();
            }
        }
        StatsManager.count("array_manager.get.invalid_length");
        return null;
    }
    
   
    public byte[] getByteArray(int length) {
        return get(length, byteArrayFactories);
    } 
    
    public short[] getShortArray(int length) {
        return get(length, shortArrayFactories);
    } 
    
    public SAMRecord[] getSAMRecordArray(int length) {
        return get(length, samRecordArrayFactories);
    } 
    
    public void returnByteArray(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        
        for (int i = 0; i < lengths.length; ++i) {
            if (bytes.length == lengths[i]) {
                byteArrayFactories[i].returnObject(bytes);
                return;
            }
        }
        StatsManager.count("array_manager.return_bytes.invalid_length");
    } 
    
    public void returnShortArray(short[] shorts) {
        if (shorts == null) {
            return;
        }
        
        for (int i = 0; i < lengths.length; ++i) {
            if (shorts.length == lengths[i]) {
                shortArrayFactories[i].returnObject(shorts);
                return;
            }
        }
        StatsManager.count("array_manager.return_shorts.invalid_length");
    } 
    
    public void returnSAMRecordArray(SAMRecord[] samRecords) {
        if (samRecords == null) {
            return;
        }
        for (int i = 0; i < lengths.length; ++i) {
            if (samRecords.length == lengths[i]) {
                samRecordArrayFactories[i].returnObject(samRecords);
                return;
            }
        }
        StatsManager.count("array_manager.return_sam_record.invalid_length");
    } 
    
    
    private class ByteArrayFactory extends ObjectFactory<byte[]> {
        
        private final int length;
        
        public ByteArrayFactory(int length) {
            super("byte[" + length + "]");
            this.length = length;
        }
        
        @Override
        public byte[] createObject() {
            return new byte[length];
        }
    }
    
    private class ShortArrayFactory extends ObjectFactory<short[]> {
        
        private final int length;
        
        public ShortArrayFactory(int length) {
            super("short[" + length + "]");
            this.length = length;
        }
        
        @Override
        public short[] createObject() {
            return new short[length];
        }
    }

    private class SAMRecordArrayFactory extends ObjectFactory<SAMRecord[]> {
    
        private final int length;
        
        public SAMRecordArrayFactory(int length) {
            super("SAMRecord[" + length + "]");
            this.length = length;
        }
        
        @Override
        public SAMRecord[] createObject() {
            return new SAMRecord[length];
        }
    }
}
