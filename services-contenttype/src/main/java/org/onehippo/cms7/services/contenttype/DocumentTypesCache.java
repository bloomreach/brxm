/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.PropertyType;

class DocumentTypesCache extends Sealable implements DocumentTypes {

    private static Map<String, String> jcrPropertyTypesMap = new HashMap<String, String>();
    static {
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_BINARY, PropertyType.TYPENAME_BINARY);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_BOOLEAN, PropertyType.TYPENAME_BOOLEAN);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_DATE, PropertyType.TYPENAME_DATE);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_DECIMAL, PropertyType.TYPENAME_DECIMAL);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_DOUBLE, PropertyType.TYPENAME_DOUBLE);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_LONG, PropertyType.TYPENAME_LONG);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_NAME, PropertyType.TYPENAME_NAME);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_PATH, PropertyType.TYPENAME_PATH);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_REFERENCE, PropertyType.TYPENAME_REFERENCE);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_STRING, PropertyType.TYPENAME_STRING);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_URI, PropertyType.TYPENAME_URI);
        jcrPropertyTypesMap.put(PropertyType.TYPENAME_WEAKREFERENCE, PropertyType.TYPENAME_WEAKREFERENCE);
        jcrPropertyTypesMap = Collections.unmodifiableMap(jcrPropertyTypesMap);
    }

    private volatile static long versionSequence = 0;

    private final long version = ++versionSequence;

    private final EffectiveNodeTypesCache entCache;
    private Map<String, String> propertyTypeMappings = new HashMap<String, String>(jcrPropertyTypesMap);
    private Map<String, DocumentTypeImpl> types = new TreeMap<String, DocumentTypeImpl>();
    private SortedMap<String, Set<DocumentType>> prefixesMap;

    protected DocumentTypesCache(EffectiveNodeTypesCache entCache) {
        this.entCache = entCache;
    }

    protected Map<String, String> getPropertyTypeMappings() {
        return propertyTypeMappings;
    }

    protected Map<String, DocumentTypeImpl> getTypes() {
        return types;
    }

    @Override
    protected void doSeal() {
        propertyTypeMappings = Collections.unmodifiableMap(propertyTypeMappings);

        for (Sealable s : types.values()) {
            s.seal();
        }

        types = Collections.unmodifiableMap(types);

        // create the prefixesMap for accessing the predefined types by their prefix
        prefixesMap = new TreeMap<String, Set<DocumentType>>();
        for (Map.Entry<String, DocumentTypeImpl> entry : types.entrySet()) {
            // recreate the prefix as the DocumentType itself may have been 'upgraded' to an aggregate without a (single) prefix
            String prefix = entry.getKey().substring(0, entry.getKey().indexOf(":"));
            Set<DocumentType> entries = prefixesMap.get(prefix);
            if (entries == null) {
                entries = new LinkedHashSet<DocumentType>();
                prefixesMap.put(prefix, entries);
            }
            entries.add(entry.getValue());
        }
        for (Map.Entry<String, Set<DocumentType>> entry : prefixesMap.entrySet()) {
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }
        prefixesMap = Collections.unmodifiableSortedMap(prefixesMap);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public DocumentTypeImpl getType(String name) {
        return types.get(name);
    }

    @Override
    public SortedMap<String, Set<DocumentType>> getTypesByPrefix() {
        return prefixesMap;
    }

    @Override
    public EffectiveNodeTypesCache getEffectiveNodeTypes() {
        return entCache;
    }
}
