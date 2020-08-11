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
package org.onehippo.cms7.crisp.api.resource;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Abstract implementation for {@link ResourceBeanMapper}.
 */
public abstract class AbstractResourceBeanMapper implements ResourceBeanMapper {

    @Override
    public <T> Collection<T> mapCollection(ResourceCollection resourceCollection, Class<T> beanType)
            throws ResourceException {
        Collection<T> targetBeanCollection = new LinkedList<>();
        mapCollection(resourceCollection, beanType, targetBeanCollection);
        return targetBeanCollection;
    }

    @Override
    public <T> Collection<T> mapCollection(ResourceCollection resourceCollection, Class<T> beanType, int offset,
            int limit) throws ResourceException {
        Collection<T> targetBeanCollection = new LinkedList<>();
        mapCollection(resourceCollection, beanType, targetBeanCollection, offset, limit);
        return targetBeanCollection;
    }

    @Override
    public <T> void mapCollection(ResourceCollection resourceCollection, Class<T> beanType,
            Collection<T> targetBeanCollection) throws ResourceException {
        mapCollection(resourceCollection, beanType, targetBeanCollection, 0, resourceCollection.size());
    }

    @Override
    public <T> void mapCollection(ResourceCollection resourceCollection, Class<T> beanType,
            Collection<T> targetBeanCollection, int offset, int length) throws ResourceException {
        if (targetBeanCollection == null) {
            throw new IllegalArgumentException("target bean collection must not be null.");
        }

        if (length == 0) {
            return;
        }

        Iterator<Resource> resourceIt = resourceCollection.iterator();

        if (offset > 0) {
            for (int i = 0; i < offset && resourceIt.hasNext(); i++) {
                resourceIt.next();
            }
        }

        final Collection<T> beanCol = targetBeanCollection;

        if (length < 0) {
            while (resourceIt.hasNext()) {
                beanCol.add(map(resourceIt.next(), beanType));
            }
        } else {
            int count = 0;
            while (resourceIt.hasNext() && count < length) {
                beanCol.add(map(resourceIt.next(), beanType));
                count++;
            }
        }
    }

}
