/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.CargoNamePath;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

class HippoCachingPathResolver implements PathResolver {

    private NameResolver nResolver;
    PathResolver pResolver;

    private final GenerationalCache<String,Path> cache;

    HippoCachingPathResolver(PathResolver pResolver, NameResolver nResolver) {
        this.pResolver = pResolver;
        this.nResolver = nResolver;
        cache = new GenerationalCache<String,Path>();
    }

    /**
     * {@inheritDoc}
     */
    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        Path qpath = cache.get(path);
        if (qpath == null) {
            qpath = HippoPathParser.parse(path, nResolver, PathFactoryImpl.getInstance());
            for (Path.Element element : qpath.getElements()) {
                if (element instanceof CargoNamePath)
                    return qpath;
            }
            cache.put(path, qpath);
        }
        return qpath;
    }

    /**
     * {@inheritDoc}
     */
    public String getJCRPath(Path qpath) throws NamespaceException {
        if(qpath instanceof CargoNamePath) {
            return pResolver.getJCRPath(qpath);
        }
        String path = qpath.getString();
        Path foundPath = cache.get(qpath.getString());
        if (foundPath == null) {
            path = pResolver.getJCRPath(qpath);
            cache.put(path, qpath);
            return path;
        } else {
            return path;
        }
    }

    public Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
        Path qpath;
        if (path.startsWith("[") && !normalizeIdentifier) {
            qpath = pResolver.getQPath(path, normalizeIdentifier);
        } else {
            qpath = cache.get(path);
            if (qpath == null) {
                qpath = HippoPathParser.parse(path, nResolver, PathFactoryImpl.getInstance());
                if(qpath instanceof CargoNamePath) {
                    return qpath;
                }
                cache.put(path, qpath);
            }
        }
        return qpath;
    }

    class GenerationalCache<K, V> extends AbstractMap<K,V> implements Map<K, V> {
        private static final int DEFAULT_CACHE_SIZE = 1000;
        private static final int DEFAULT_SIZE_AGE_RATIO = 10;
        private final int maxSize;
        private final int maxAge;
        private Map<K, V> cache = new HashMap<K, V>();
        private Map<K, V> old = new HashMap<K, V>();
        private Map<K, V> young = new HashMap<K, V>();
        private int age = 0;

        public GenerationalCache() {
            this.maxSize = DEFAULT_CACHE_SIZE;
            this.maxAge = DEFAULT_CACHE_SIZE / DEFAULT_SIZE_AGE_RATIO;
        }

        @Override
        public synchronized V get(Object key) {
            K safeKey = (K)key;
            V value = cache.get(safeKey);
            if (value == null) {
                value = old.get(safeKey);
                if (value != null) {
                    put(safeKey, value);
                }
            }
            return value;
        }

        @Override
        public synchronized V put(K key, V value) {
            young.put(key, value);

            if (++age == maxAge) {
                Map<K,V> union = new HashMap<K,V>();
                for(Map.Entry<K,V> entry : old.entrySet()) {
                    if (young.containsKey(entry.getKey())) {
                        union.put(entry.getKey(), entry.getValue());
                    }
                }

                if (!union.isEmpty()) {
                    if (cache.size() + union.size() <= maxSize) {
                        union.putAll(cache);
                    }
                    cache = union;
                }

                old = young;
                young = new HashMap<K,V>();
                age = 0;
            }

            return null;
        }

        @Override
        public synchronized Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K,V>> entrySet = new TreeSet<Map.Entry<K,V>>();
            entrySet.addAll(cache.entrySet());
            entrySet.addAll(old.entrySet());
            entrySet.addAll(young.entrySet());
            return entrySet;
        }
    }
}
