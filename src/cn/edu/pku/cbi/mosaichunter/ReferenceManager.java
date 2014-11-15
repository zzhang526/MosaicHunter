package cn.edu.pku.cbi.mosaichunter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReferenceManager implements Serializable {
    
/*
    public String[] DEFAULT_VALID_REFERENCE = new String[] {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", 
            "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y"  
    };
*/
    
    private static final long serialVersionUID = 1L;

    public static final int BUFFER_SIZE = 100 * 1024 * 1024;
    
    public static final byte[] id2Base = new byte[] {'A', 'C', 'G', 'T'};
    public static final int[] base2Id = new int[256];
    
    static {
        for (int i = 0; i < 256; ++i) {
            base2Id[i] = -1;
        }
        base2Id['A'] = base2Id['a'] = 0;
        base2Id['C'] = base2Id['c'] = 1;
        base2Id['G'] = base2Id['g'] = 2;
        base2Id['T'] = base2Id['t'] = 3;
    }
     
    private final Map<String, Integer> referenceIds = new HashMap<String, Integer>();
    private final ArrayList<Reference> references = new ArrayList<Reference>();
    
    public ReferenceManager(String referenceFastaFile) throws IOException {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(referenceFastaFile));
            parse(r);
        } finally {
            if (r != null) {
                r.close();
            }
        }
       
    }

    private void parse(BufferedReader r) throws IOException {
        long[] buffer = new long[BUFFER_SIZE];
        Reference reference = null;
        Sequence sequence = null;
        for (;;) {
            String l = r.readLine();
            if (l == null || (!l.isEmpty() && l.charAt(0) == '>')) {
                if (reference != null) {
                    if (sequence != null) {
                        sequence.length = reference.length - sequence.start;
                        sequence.bases = new long[(sequence.length >> 5) + 1];
                        System.arraycopy(
                                buffer, 0, sequence.bases, 0, sequence.bases.length);
                        reference.sequences.add(sequence);
                        sequence = null;
                    }
                    referenceIds.put(reference.name, references.size());
                    references.add(reference);
                }
            }
            if (l == null) {
                break;
            }
            if (l.isEmpty()) {
                continue;
            }
            
            if (l.charAt(0) == '>') {
                int p1 = 1;
                while (p1 < l.length() && l.charAt(p1) == '>') {
                    p1++;
                }
                int p2 = p1;
                while (p2 < l.length() && l.charAt(p2) != ' ') {
                    p2++;
                }
                reference = new Reference();
                reference.metadata = l.substring(p1);
                if (p2 == l.length()) {
                    reference.name = reference.metadata;
                } else {
                    reference.name = l.substring(p1, p2);
                }
                reference.length = 0;
                reference.sequences = new ArrayList<Sequence>();
            } else {
                for (int i = 0; i < l.length(); ++i, ++reference.length) {
                    long id = base2Id[l.charAt(i)];
                    if (id < 0) {
                        if (sequence != null) {
                            sequence.length = reference.length - sequence.start;
                            sequence.bases = new long[(sequence.length >> 5) + 1];
                            System.arraycopy(
                                    buffer, 0, sequence.bases, 0, sequence.bases.length);
                            reference.sequences.add(sequence);
                            sequence = null;
                        }
                    } else {
                        if (sequence == null) {
                            sequence = new Sequence();
                            sequence.start = reference.length;
                        }
                        int offset = reference.length - sequence.start;
                        int o1 = (offset & 31) << 1;
                        int o2 = 64 - o1 - 2;
                        buffer[offset >> 5] = ((buffer[offset >> 5] << o2) >>> o2) & ((1L << o1) - 1) | (id << o1);
                    }
                }
            }
        }
    }
    
    public void toFasta(String fileName) throws IOException {
        BufferedWriter w = new BufferedWriter(new FileWriter(fileName));
        for (Reference r : references) {
            int c = 0;
            w.write(">" + r.metadata);
            w.newLine();
            int p = 0;
            for (Sequence s : r.sequences) {
                for (int i = p; i < s.start; ++i) {
                    if (c > 0 && c % 60 == 0) {
                        w.newLine();
                    }
                    c++;
                    w.write('N');
                }
                for (int i = 0; i < s.length; ++i) {
                    if (c > 0 && c % 60 == 0) {
                        w.newLine();
                    }
                    c++;
                    w.write(id2Base[(int) ((s.bases[i >> 5] >> ((i & 31) << 1)) & 3)]);
                }
                p = s.start + s.length;
            }
            for (int i = p; i < r.length; ++i) {
                if (c > 0 && c % 60 == 0) {
                    w.newLine();
                }
                c++;
                w.write('N');
            }
            w.newLine();
        }
        w.close();
    }
    
    public int getReferenceId(String referenceName) {
        Integer id = referenceIds.get(referenceName);
        if (id == null) {
            return -1;
        }
        return id;
    }
    
    public byte getBase(int referenceId, long position) {
        Reference reference = references.get(referenceId);
        for (Sequence s : reference.sequences) {
            if (position > s.start && position <= s.start + s.length) {
                long offset = position - s.start - 1;
                return id2Base[(int) ((s.bases[(int) (offset >>> 5)] >> ((offset & 31) << 1)) & 3)];
            }
        }
        return 'N';
    }

    public byte getBase(String referenceName, long position) {
        int id = getReferenceId(referenceName);
        if (id < 0) {
            throw new IllegalArgumentException("invalid reference name: " + referenceName);
        }
        return getBase(id, position);
    }
  
    private class Reference implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private String metadata;
        private int length;
        List<Sequence> sequences;
    }
    
    private class Sequence implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int start;
        private int length;
        private long[] bases;
    }
}

