/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.request;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Optional;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterAware;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * delegatee is shared by multiple threads. CachingObjectConverter is request bound and used single threaded
 * (thread-safe)
 */
class CachingObjectConverter implements ObjectConverter {

    private static final Logger log = LoggerFactory.getLogger(CachingObjectConverter.class);

    private final ObjectConverter delegatee;

    private final ObjectCache objectCache = new ObjectCache();

    protected CachingObjectConverter(ObjectConverter delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public String getPrimaryObjectType(final Node node) throws ObjectBeanManagerException {
        return delegatee.getPrimaryObjectType(node);
    }

    @Override
    public Object getObject(final Session session, final String path) throws ObjectBeanManagerException {
        if(StringUtils.isEmpty(path) || !path.startsWith("/")) {
            log.warn("Illegal argument for '{}' : not an absolute path", path);
            return null;
        }
        CacheKey key = new CacheKey(session, path);
        Optional<Object> cached = objectCache.get(key);
        if (cached != null) {
            return cached.orNull();
        }
        Object o = delegatee.getObject(session, path);
        setObjectConverter(o);
        objectCache.put(key, o);
        return o;
    }

    @Override
    public Object getObject(final Node node) throws ObjectBeanManagerException {
        CacheKey key = new CacheKey(node);
        Optional<Object> cached = objectCache.get(key);
        if (cached != null) {
            return cached.orNull();
        }
        Object o = delegatee.getObject(node);
        setObjectConverter(o);
        objectCache.put(key, o);
        return o;
    }

    @Override
    public Object getObject(final Node node, final String relPath) throws ObjectBeanManagerException {
        if(StringUtils.isEmpty(relPath) || relPath.startsWith("/")) {
            log.warn("'{}' is not a valid relative path. Return null.", relPath);
            return null;
        }
        if(node == null) {
            log.warn("Node is null. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        CacheKey key = new CacheKey(node, relPath);
        Optional<Object> cached = objectCache.get(key);
        if (cached != null) {
            return cached.orNull();
        }
        Object o = delegatee.getObject(node, relPath);
        setObjectConverter(o);
        objectCache.put(key, o);
        return o;
    }

    @Override
    public Object getObject(final String uuid, final Session session) throws ObjectBeanManagerException {
        CacheKey key = new CacheKey(session, uuid);
        Optional<Object> cached = objectCache.get(key);
        if (cached != null) {
            return cached.orNull();
        }
        Object o = delegatee.getObject(uuid, session);
        setObjectConverter(o);
        objectCache.put(key, o);
        return o;
    }

    @Override
    public Object getObject(final String uuid, final Node node) throws ObjectBeanManagerException {
        CacheKey key = new CacheKey(node, uuid);
        Optional<Object> cached = objectCache.get(key);
        if (cached != null) {
            return cached.orNull();
        }
        Object o = delegatee.getObject(uuid, node);
        setObjectConverter(o);
        objectCache.put(key, o);
        return o;
    }

    @Override
    public Class<? extends HippoBean> getAnnotatedClassFor(final String jcrPrimaryNodeType) {
        return delegatee.getAnnotatedClassFor(jcrPrimaryNodeType);
    }

    @Override
    public String getPrimaryNodeTypeNameFor(final Class<? extends HippoBean> hippoBean) {
        return delegatee.getPrimaryNodeTypeNameFor(hippoBean);
    }

    private void setObjectConverter(final Object o) {
        if (o instanceof ObjectConverterAware) {
            ((ObjectConverterAware) o).setObjectConverter(this);
        }
    }

    private class ObjectCache {
        private final Map<CacheKey, Optional<Object>> cache = new HashMap<CacheKey, Optional<Object>>();

        public Optional<Object> get(final CacheKey key) {
            return cache.get(key);
        }

        /**
         * puts objects in the cache, also <code>null</code> values
         */
        public void put(final CacheKey key, final Object object) {
            if (object == null) {
                cache.put(key, Optional.absent());
            } else {
                cache.put(key, Optional.of(object));
            }
        }
    }

    private class CacheKey {
        final String sessionUserId;
        final String pathOrUuid;

        private CacheKey(final Session session, final String pathOrUuid) {
            this.sessionUserId = session.getUserID();
            this.pathOrUuid = pathOrUuid;
        }

        private CacheKey(Node node) throws ObjectBeanManagerException {
            try {
                this.sessionUserId = node.getSession().getUserID();
                this.pathOrUuid = node.getPath();
            } catch (RepositoryException e) {
                throw new ObjectBeanManagerException(e);
            }
        }

        private CacheKey(Node node, String relPath) throws ObjectBeanManagerException {
            try {
                this.sessionUserId = node.getSession().getUserID();
                this.pathOrUuid = node.getPath() + "/" + relPath;
            } catch (RepositoryException e) {
                throw new ObjectBeanManagerException(e);
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CacheKey cacheKey = (CacheKey) o;

            if (!pathOrUuid.equals(cacheKey.pathOrUuid)) {
                return false;
            }
            if (!sessionUserId.equals(cacheKey.sessionUserId)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = sessionUserId.hashCode();
            result = 31 * result + pathOrUuid.hashCode();
            return result;
        }
    }
}
