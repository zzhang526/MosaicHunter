/*
 * The MIT License
 *
 * Copyright (c) 2016 Center for Bioinformatics, Peking University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package cn.edu.pku.cbi.mosaichunter.reference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.edu.pku.cbi.mosaichunter.MosaicHunterHelper;

public class ReferenceManager implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public static final int BUFFER_SIZE = 100 * 1024 * 1024;
    
    private final Map<String, Integer> referenceIds = new HashMap<String, Integer>();
    private final List<Reference> references = new ArrayList<Reference>();
    private long totalLength = 0;
    private final Set<String> validReferences = new HashSet<String>();
    
    private int lastReferenceId;
    private long lastPosition;
    private Sequence lastSequence;
    
    public ReferenceManager(String referenceFastaFile, String[] validReferences) 
            throws IOException {
        if (validReferences != null) {
            this.validReferences.addAll(Arrays.asList(validReferences));
        }
        
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

        long sequenceStart = 0;
        String referenceName = null;
        String referenceMetadata = null;
        long referenceLength = 0;
        List<Sequence> referenceSequences = null;
                
        for (;;) {
            String l = r.readLine();
            if (l == null || (!l.isEmpty() && l.charAt(0) == '>')) {
                if (referenceName != null) {
                    if (sequenceStart > 0) {
                        long sequenceLength = referenceLength - sequenceStart;
                        Sequence sequence = new Sequence(
                                sequenceStart, 
                                sequenceLength,
                                new long[(int)((sequenceLength >> 5) + 1)]);
                        System.arraycopy(
                                buffer, 0, sequence.getBases(), 0, sequence.getBases().length);
                        referenceSequences.add(sequence);
                        sequenceStart = 0;
                    }
                    
                    if (validReferences.isEmpty() || validReferences.contains(referenceName)) {
                        referenceIds.put(referenceName, references.size());
                        Reference reference = new Reference(
                                referenceName, referenceMetadata, 
                                referenceLength, referenceSequences);
                        references.add(reference);
                        totalLength += reference.getLength();
                    }
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
                referenceMetadata = l.substring(p1);
                if (p2 == l.length()) {
                    referenceName = referenceMetadata;
                } else {
                    referenceName = l.substring(p1, p2);
                }
                referenceLength = 0;
                referenceSequences = new ArrayList<Sequence>();
            } else {
                if (referenceName != null) {
                    for (char c : l.toCharArray()) {
                        long id = MosaicHunterHelper.BASE_TO_ID[c];
                        if (id < 0) {
                            if (sequenceStart > 0) {
                                long sequenceLength = referenceLength - sequenceStart;
                                Sequence sequence = new Sequence(
                                        sequenceStart, 
                                        sequenceLength,
                                        new long[(int)((sequenceLength >> 5) + 1)]);
                                System.arraycopy(
                                        buffer, 0, 
                                        sequence.getBases(), 0, sequence.getBases().length);
                                referenceSequences.add(sequence);
                                sequenceStart = 0;
                            }
                        } else {
                            if (sequenceStart == 0) {
                                sequenceStart = referenceLength;
                            }
                            long offset = referenceLength - sequenceStart;
                            int o1 = ((int) (offset & 31)) << 1;
                            int o2 = 64 - o1 - 2;
                            int k = (int) (offset >> 5);
                            buffer[k] = ((buffer[k] << o2) >>> o2) & ((1L << o1) - 1) | (id << o1);
                        }
                        ++referenceLength;
                    }
                }
            }
        }
    }
    
    public int getReferenceNumber() {
        return referenceIds.size();
    }
    
    public long getTotalLength() {
        return totalLength;
    }
    
    public Reference getReference(String referenceName) {
        Integer id = referenceIds.get(referenceName);
        if (id == null) {
            return null;
        }
        return getReference(id);
    }
    
    public Reference getReference(int referenceId) {
        return references.get(referenceId);
    }
    
    public List<Reference> getReferences() {
        return new ArrayList<Reference>(references);
    }
    
    public long getReferenceLength(String referenceName) {
        Reference ref = getReference(referenceName);
        if (ref == null) {
            return -1;
        }
        return ref.getLength();
    }
    
    public long getReferenceLength(int referenceId) {
        Reference ref = getReference(referenceId);
        if (ref == null) {
            return -1;
        }
        return ref.getLength();
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
        for (Sequence s : reference.getSequences()) {
            if (position > s.getStart() && position <= s.getStart() + s.getLength()) {
                return MosaicHunterHelper.ID_TO_BASE[getBaseId(s, position)];
            }
        }
        return 'N';
    }
    
    public byte getBaseWithCache(int referenceId, long position) {
        if (lastSequence != null && 
            lastReferenceId == referenceId && 
            (position == lastPosition + 1 || position == lastPosition)) {
            byte ret = MosaicHunterHelper.ID_TO_BASE[getBaseId(lastSequence, position)];
            if (position < lastSequence.getStart() + lastSequence.getLength()) {
                lastPosition = position;
            } else {
                lastSequence = null;
            }
            return ret;
        }
        Reference reference = references.get(referenceId);
        for (Sequence s : reference.getSequences()) {
            if (position > s.getStart() && position <= s.getStart() + s.getLength()) {
                if (position < s.getStart() + s.getLength()) {
                    lastSequence = s;
                    lastReferenceId = referenceId;
                    lastPosition = position;
                }
                return MosaicHunterHelper.ID_TO_BASE[getBaseId(s, position)];
            }
        }
        
        return 'N';
    }
    
    private static int getBaseId(Sequence s, long position) {
        long offset = position - s.getStart() - 1;
        return (int) ((s.getBases()[(int) (offset >>> 5)] >> ((offset & 31) << 1)) & 3);
    }
    
    public byte getBase(String referenceName, long position) {
        int id = getReferenceId(referenceName);
        if (id < 0) {
            throw new IllegalArgumentException("invalid reference name: " + referenceName);
        }
        return getBase(id, position);
    }
  
}

