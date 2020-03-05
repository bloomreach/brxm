/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ContentTypesProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanInterceptor;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import static org.hippoecm.hst.util.ObjectConverterUtils.DEFAULT_FALLBACK_NODE_TYPES;
import static org.hippoecm.hst.util.ObjectConverterUtils.getAggregatedMapping;
import static org.hippoecm.hst.util.ObjectConverterUtils.getInterceptorMapping;

/**
 * A proxy, which keeps a cache of instantiated object converters per content types.
 * If no object converter exists for corresponding ContentTypes, it will be instantiated and put into a cache.
 * ContentTypes instance is stored as a weak reference, so eventually, cache entry will be invalidated as soon as there will
 * be no strong references to ContentTypes
 * @see ObjectConverter
 * @see ContentTypes
 */
public class VersionedObjectConverterProxy implements ObjectConverter {

    private final static Logger log = LoggerFactory.getLogger(VersionedObjectConverterProxy.class);

    private final Cache<ContentTypes, ObjectConverter> instanceCache = CacheBuilder.newBuilder().weakKeys().build();

    private final ContentTypesProvider contentTypesProvider;
    private final Map<String, Class<? extends HippoBean>> jcrNodeTypeClassPairs;
    private final Map<String, Class<? extends DynamicBeanInterceptor>> dynamicBeanInterceptorPairs;

    public VersionedObjectConverterProxy(final Collection<Class<? extends HippoBean>> annotatedNodeClasses,
            final Collection<Class<? extends DynamicBeanInterceptor>> annotatedInterceptorClasses,
            final ContentTypesProvider contentTypesProvider) {
        this(annotatedNodeClasses, annotatedInterceptorClasses, contentTypesProvider, false);
    }

    public VersionedObjectConverterProxy(final Collection<Class<? extends HippoBean>> annotatedNodeClasses,
            final Collection<Class<? extends DynamicBeanInterceptor>> annotatedInterceptorClasses,
            final ContentTypesProvider contentTypesProvider, final boolean ignoreDuplicates) {
        this.contentTypesProvider = contentTypesProvider;
        this.jcrNodeTypeClassPairs = getAggregatedMapping(annotatedNodeClasses, ignoreDuplicates);
        this.dynamicBeanInterceptorPairs = getInterceptorMapping(annotatedInterceptorClasses, ignoreDuplicates);
    }

    /**
     * Create or get content type version specific instance of {@link ObjectConverter}
     */
    private ObjectConverter getOrCreateObjectConverter() throws ObjectBeanManagerException {
        final ContentTypes contentTypes = contentTypesProvider.getContentTypes();
        try {
            return instanceCache.get(contentTypes, () -> new DynamicObjectConverterImpl(jcrNodeTypeClassPairs,
                    dynamicBeanInterceptorPairs, DEFAULT_FALLBACK_NODE_TYPES, contentTypes));
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not create ObjectConverter", e);
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
        try {
            return getOrCreateObjectConverter().getClassFor(jcrPrimaryNodeType);
        } catch (ObjectBeanManagerException e) {
            log.error("Could not get annoted class for '{}'", jcrPrimaryNodeType, e);
            return null;
        }
    }

    @Override
    public String getPrimaryNodeTypeNameFor(final Class<? extends HippoBean> hippoBean) {
        try {
            return getOrCreateObjectConverter().getPrimaryNodeTypeNameFor(hippoBean);
        } catch (ObjectBeanManagerException e) {
            log.error("Could not get annoted nodetype", e);
            return null;
        }
    }
}
