/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.manager;

import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ContentTypesProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.onehippo.cms7.services.contenttype.ContentTypes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import static org.hippoecm.hst.util.ObjectConverterUtils.DEFAULT_FALLBACK_NODE_TYPES;

/**
 * A proxy, which keeps a cache of instantiated object converters per content types.
 * If no object converter exists for corresponding ContentTypes, it will be instantiated and put into a cache.
 * ContentTypes instance is stored as a weak reference, so eventually, cache entry will be invalidated as soon as there will
 * be no strong references to ContentTypes
 * @see ObjectConverter
 * @see ContentTypes
 */
public class VersionedObjectConverterProxy implements ObjectConverter {

    private final Cache<ContentTypes, ObjectConverter> instanceCache = CacheBuilder.newBuilder().weakKeys().build();

    private final ContentTypesProvider contentTypesProvider;
    private final Collection<Class<? extends HippoBean>> annotatedClasses;
    private final boolean ignoreDuplicates;

    public VersionedObjectConverterProxy(Collection<Class<? extends HippoBean>> annotatedClasses, final ContentTypesProvider contentTypesProvider) {
        this(annotatedClasses, contentTypesProvider, false);
    }

    public VersionedObjectConverterProxy(Collection<Class<? extends HippoBean>> annotatedClasses, final ContentTypesProvider contentTypesProvider, final boolean ignoreDuplicates) {
        this.annotatedClasses = annotatedClasses;
        this.contentTypesProvider = contentTypesProvider;
        this.ignoreDuplicates = ignoreDuplicates;
    }

    /**
     * Create or get content type version specific instance of {@link ObjectConverter}
     */
    private ObjectConverter getOrCreateObjectConverter() {

        final ContentTypes contentTypes = contentTypesProvider.getContentTypes();
        final ObjectConverter objectConverter = instanceCache.getIfPresent(contentTypes);
        if (objectConverter != null) {
            return objectConverter;
        } else {
            final Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeExistingClassPairs = ObjectConverterUtils
                    .getAggregatedMapping(annotatedClasses, null, ignoreDuplicates);
            final Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs =
                    ObjectConverterUtils.getAggregatedMapping(annotatedClasses, ignoreDuplicates);
            final ObjectConverter converter = new DynamicObjectConverterImpl(jcrPrimaryNodeTypeClassPairs, jcrPrimaryNodeTypeExistingClassPairs,
                    DEFAULT_FALLBACK_NODE_TYPES, contentTypes);
            instanceCache.put(contentTypes, converter);
            return converter;
        }
    }

    @Override
    public String getPrimaryObjectType(final Node node) throws ObjectBeanManagerException {
        return getOrCreateObjectConverter().getPrimaryObjectType(node);
    }

    @Override
    public Object getObject(final Session session, final String path) throws ObjectBeanManagerException {
        return getOrCreateObjectConverter().getObject(session, path);
    }

    @Override
    public Object getObject(final Node node) throws ObjectBeanManagerException {
        return getOrCreateObjectConverter().getObject(node);
    }

    @Override
    public Object getObject(final Node node, final String relPath) throws ObjectBeanManagerException {
        return getOrCreateObjectConverter().getObject(node, relPath);
    }

    @Override
    public Object getObject(final String uuid, final Session session) throws ObjectBeanManagerException {
        return getOrCreateObjectConverter().getObject(uuid, session);
    }

    @Override
    public Object getObject(final String uuid, final Node node) throws ObjectBeanManagerException {
        return getOrCreateObjectConverter().getObject(uuid, node);
    }

    @Override
    public Class<? extends HippoBean> getAnnotatedClassFor(final String jcrPrimaryNodeType) {
        return getOrCreateObjectConverter().getClassFor(jcrPrimaryNodeType);
    }

    @Override
    public String getPrimaryNodeTypeNameFor(final Class<? extends HippoBean> hippoBean) {
        return getOrCreateObjectConverter().getPrimaryNodeTypeNameFor(hippoBean);
    }
}
