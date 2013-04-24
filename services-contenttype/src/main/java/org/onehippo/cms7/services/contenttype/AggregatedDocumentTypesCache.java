/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code is forked from the Jackrabbit 2.6 org.apache.jackrabbit.core.nodetype.BitSetENTCacheImpl class
 * and heavily modified and reduced for usage by the HippoContentTypeService
 */
package org.onehippo.cms7.services.contenttype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements an effective DocumentTypeImpl cache of all DocumentTypes and aggregated versions thereof, using a bit set for storing the
 * information about participating DocumentTypes in a set.
 */
public class AggregatedDocumentTypesCache {

    /**
     * constant for bits-per-word
     */
    private static final int BPW = 64;

    /**
     * OR mask for bit set
     */
    private static final long[] OR_MASK = new long[BPW];
    static {
        for (int i = 0; i < BPW; i++) {
            OR_MASK[i] = 1L << i;
        }
    }

    private final ReentrantReadWriteLock readWriteLock =  new ReentrantReadWriteLock();
    private final Lock read  = readWriteLock.readLock();
    private final Lock write = readWriteLock.writeLock();


    private final DocumentTypesCache dtCache;

    /**
     * An ordered set of the keys. This is used for {@link #findBest(Key)}.
     */
    private final Map<Integer, TreeSet<Key>> sortedKeys;

    /**
     * cache of pre-built aggregations of node types
     */
    private final HashMap<Key, DocumentTypeImpl> aggregates;

    /**
     * A lookup table for bit numbers for a given name.
     *
     * Note: usage of this map isn't synchronized under the assumption that
     * the initial seeding of the cache will register all registered names after which
     * no modification of this map will occur anymore.
     */
    private final HashMap<String, Integer> nameIndex = new HashMap<String, Integer>();

    /**
     * The reverse lookup table for bit numbers to names
     */
    private String[] names = new String[1024];

    /**
     * Creates a new bitset effective node type cache
     */
    public AggregatedDocumentTypesCache(DocumentTypesCache dtCache) {
        this.dtCache = dtCache;
        sortedKeys = new HashMap<Integer, TreeSet<Key>>();
        aggregates = new HashMap<Key, DocumentTypeImpl>();
    }

    public DocumentTypesCache getDocumentTypesCache() {
        return dtCache;
    }

    public Key getKey(String name) {
        return new Key(name);
    }

    public Key getKey(Set<String> names) {
        return new Key(names);
    }

    public DocumentTypeImpl put(DocumentTypeImpl dt) {
        return put(getKey(dt.getAggregatedTypes()), dt);
    }

    public Set<Key> getKeys() {
        return aggregates.keySet();
    }

    public DocumentTypeImpl put(Key key, DocumentTypeImpl dt) {
        DocumentTypeImpl existing = get(key);
        if (existing != null) {
            // don't overwrite an existing element, return what already was stored
            return existing;
        }
        aggregates.put(key, dt);

        write.lock();
        try {
            TreeSet<Key> keys = sortedKeys.get(key.numBits);
            if (keys == null) {
                keys = new TreeSet<Key>();
                sortedKeys.put(key.numBits, keys);
            }
            keys.add(key);
        }
        finally {
            write.unlock();
        }
        return dt;
    }

    public Key findBest(Key key) {
        // quick check for already cached key
        if (contains(key)) {
            return key;
        }

        int bits = key.numBits -1;
        if (bits > 0) {
            read.lock();
            try {
                while (bits > 0) {
                    TreeSet<Key> keys = sortedKeys.get(bits);
                    if (keys != null) {
                        for (Key k : keys) {
                            if (key.contains(k)) {
                                return k;
                            }
                        }
                    }
                    bits--;
                }
            }
            finally {
                read.unlock();
            }
        }

        return null;
    }

    public boolean contains(Key key) {
        return aggregates.containsKey(key);
    }

    public DocumentTypeImpl get(Key key) {
        return aggregates.get(key);
    }

    public DocumentTypeImpl get(String name) {
        return aggregates.get(getKey(name));
    }

    public DocumentTypeImpl get(Set<String> names) {
        return aggregates.get(getKey(names));
    }

    /**
     * Defines a {@link Key} by storing the node type aggregate information
     * in a bit set. We do not use the {@link java.util.BitSet} because it
     * does not suit all our requirements. Every node type is represented by a bit
     * in the set. This key is immutable.
     */
    public class Key implements Comparable<Key> {

        /**
         * The number of node types that form this key.
         */
        private final int numBits;

        /**
         * The array of longs that hold the bit information.
         */
        private final long[] bits;

        /**
         * the hash code, only calculated once
         */
        private final int hashCode;

        /**
         * Creates a new bit set key.
         * @param name the node type name
         */
        public Key(String name) {
            this.numBits = 1;
            int maxBit = nameIndex.size() + 1;
            bits = new long[maxBit / BPW + 1];

            int n = getBitNumber(name);
            bits[n / BPW] |= OR_MASK[n % BPW];
            hashCode = calcHashCode();
        }

        /**
         * Creates a new bit set key.
         * @param names the node type names
         */
        public Key(Set<String> names) {
            this.numBits = names.size();
            int maxBit = nameIndex.size() + this.numBits;
            bits = new long[maxBit / BPW + 1];

            for (String name : names) {
                int n = getBitNumber(name);
                bits[n / BPW] |= OR_MASK[n % BPW];
            }
            hashCode = calcHashCode();
        }

        /**
         * Creates a new bit set key.
         * @param bits the array of bits
         * @param numBits the number of bits that are '1' in the given bits
         */
        private Key(long[] bits, int numBits) {
            this.bits = bits;
            this.numBits = numBits;
            hashCode = calcHashCode();
        }

        public Set<String> getNames() {
            Set<String> set = new HashSet<String>(numBits);
            int i = nextSetBit(0);
            while (i >= 0) {
                set.add(names[i]);
                i = nextSetBit(i + 1);
            }
            return set;
        }

        public boolean contains(Key other) {
            /*
             * 0 - 0 => 0
             * 0 - 1 => 1
             * 1 - 0 => 0
             * 1 - 1 => 0
             * !a and b
             */
            int len = Math.max(bits.length, other.bits.length);
            for (int i = 0; i < len; i++) {
                long w1 = i < bits.length ? bits[i] : 0;
                long w2 = i < other.bits.length ? other.bits[i] : 0;
                long r = ~w1 & w2;
                if (r != 0) {
                    return false;
                }
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Key subtract(Key other) {
            /*
             * 0 - 0 => 0
             * 0 - 1 => 0
             * 1 - 0 => 1
             * 1 - 1 => 0
             * a and !b
             */
            int len = Math.max(bits.length, other.bits.length);
            long[] newBits = new long[len];
            int numBits = 0;
            for (int i = 0; i < len; i++) {
                long w1 = i < bits.length ? bits[i] : 0;
                long w2 = i < other.bits.length ? other.bits[i] : 0;
                newBits[i] = w1 & ~w2;
                numBits += bitCount(newBits[i]);
            }
            return new Key(newBits, numBits);
        }

        /**
         * Returns the bit number for the given name. If the name does not exist
         * a new new bit number for that name is created.
         *
         * @param name the name to lookup
         * @return the bit number for the given name
         */
        private int getBitNumber(String name) {
            Integer i = (Integer) nameIndex.get(name);
            if (i == null) {
                int idx = nameIndex.size();
                i = new Integer(idx);
                nameIndex.put(name, i);
                if (idx >= names.length) {
                    String[] newNames = new String[names.length * 2];
                    System.arraycopy(names, 0, newNames, 0, names.length);
                    names = newNames;
                }
                names[idx] = name;
            }
            return i.intValue();
        }

        /**
         * Returns the bit number of the next bit that is set, starting at
         * <code>fromIndex</code> inclusive.
         *
         * @param fromIndex the bit position to start the search
         * @return the bit position of the bit or -1 if none found.
         */
        private int nextSetBit(int fromIndex) {
            int addr = fromIndex / BPW;
            int off = fromIndex % BPW;
            while (addr < bits.length) {
                if (bits[addr] != 0) {
                    while (off < BPW) {
                        if ((bits[addr] & OR_MASK[off]) != 0) {
                            return addr * BPW + off;
                        }
                        off++;
                    }
                    off = 0;
                }
                addr++;
            }
            return -1;
        }

        /**
          * Returns the number of bits set in val.
          * For a derivation of this algorithm, see
          * "Algorithms and data structures with applications to
          *  graphics and geometry", by Jurg Nievergelt and Klaus Hinrichs,
          *  Prentice Hall, 1993.
          *
          * @param val the value to calculate the bit count for
          * @return the number of '1' bits in the value
          */
         private int bitCount(long val) {
             val -= (val & 0xaaaaaaaaaaaaaaaaL) >>> 1;
             val =  (val & 0x3333333333333333L) + ((val >>> 2) & 0x3333333333333333L);
             val =  (val + (val >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
             val += val >>> 8;
             val += val >>> 16;
             return ((int) (val) + (int) (val >>> 32)) & 0xff;
         }

        public int compareTo(Key o) {
            int res = o.numBits - numBits;
            if (res == 0) {
                int adr = Math.max(bits.length, o.bits.length) - 1;
                while (adr >= 0) {
                    long w1 = adr < bits.length ? bits[adr] : 0;
                    long w2 = adr < o.bits.length ? o.bits[adr] : 0;
                    if (w1 != w2) {
                        // some signed arithmetic here
                        long h1 = w1 >>> 32;
                        long h2 = w2 >>> 32;
                        if (h1 == h2) {
                            h1 = w1 & 0xffffffffL;
                            h2 = w2 & 0xffffffffL;
                        }
                        return Long.signum(h2 - h1);
                    }
                    adr--;
                }
            }
            return res;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Key) {
                Key o = (Key) obj;
                if (numBits != o.numBits) {
                    return false;
                }
                int adr = Math.max(bits.length, o.bits.length) - 1;
                while (adr >= 0) {
                    long w1 = adr < bits.length ? bits[adr] : 0;
                    long w2 = adr < o.bits.length ? o.bits[adr] : 0;
                    if (w1 != w2) {
                        return false;
                    }
                    adr--;
                }
                return true;
            }
            return false;
        }

        public int hashCode() {
            return hashCode;
        }

        /**
         * Calculates the hash code.
         * @return the calculated hash code
         */
        private int calcHashCode() {
            long h = 1234;
            int addr = bits.length - 1;
            while (addr >= 0 && bits[addr] == 0) {
                addr--;
            }
            while (addr >= 0) {
                h ^= bits[addr] * (addr + 1);
                addr--;
            }
            return (int) ((h >> 32) ^ h);
        }

        public String toString() {
            StringBuilder buf = new StringBuilder("names=[");
            int i = nextSetBit(0);
            while (i >= 0) {
                buf.append(names[i]);
                i = nextSetBit(i + 1);
                buf.append( i >= 0 ? "," : "]");
            }
            return buf.toString();
        }
    }
}
