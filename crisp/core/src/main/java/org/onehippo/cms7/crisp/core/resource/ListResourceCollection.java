/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceCollection;

/**
 * {@link ResourceCollection} implementation based on an underlying List object.
 */
public class ListResourceCollection implements ResourceCollection {

    /**
     * Unmodifiable view of the internal resource list.
     */
    private final List<Resource> unmodifiableList;

    /**
     * Constructs with a internal resource list.
     * @param list internal resource list
     */
    public ListResourceCollection(final List<Resource> list) {
        super();
        unmodifiableList = Collections.unmodifiableList(list);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Resource> iterator() {
        return unmodifiableList.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Resource> getCollection() {
        return unmodifiableList;
    }

    @Override
    public int size() {
        return unmodifiableList.size();
    }

    @Override
    public Resource get(int index) {
        return unmodifiableList.get(index);
    }

}