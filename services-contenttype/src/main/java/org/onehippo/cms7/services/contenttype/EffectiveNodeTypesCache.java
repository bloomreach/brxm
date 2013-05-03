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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

class EffectiveNodeTypesCache extends Sealable implements EffectiveNodeTypes {

    private volatile static long versionSequence = 0;

    private final long version = ++versionSequence;

    private Map<String, EffectiveNodeTypeImpl> types = new TreeMap<String, EffectiveNodeTypeImpl>();
    private SortedMap<String, Set<EffectiveNodeType>> prefixesMap;

    protected Map<String, EffectiveNodeTypeImpl> getTypes() {
        return types;
    }

    protected void doSeal() {
        for (Sealable s : types.values()) {
            s.seal();
        }

        types = Collections.unmodifiableMap(types);

        prefixesMap = new TreeMap<String, Set<EffectiveNodeType>>();
        for (Map.Entry<String, EffectiveNodeTypeImpl> entry : types.entrySet()) {
            Set<EffectiveNodeType> entries = prefixesMap.get(entry.getValue().getPrefix());
            if (entries == null) {
                entries = new LinkedHashSet<EffectiveNodeType>();
                prefixesMap.put(entry.getValue().getPrefix(), entries);
            }
            entries.add(entry.getValue());
        }
        for (Map.Entry<String, Set<EffectiveNodeType>> entry : prefixesMap.entrySet()) {
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }

        prefixesMap = Collections.unmodifiableSortedMap(prefixesMap);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public EffectiveNodeType getType(String name) {
        return types.get(name);
    }

    @Override
    public SortedMap<String, Set<EffectiveNodeType>> getTypesByPrefix() {
        return prefixesMap;
    }
}
