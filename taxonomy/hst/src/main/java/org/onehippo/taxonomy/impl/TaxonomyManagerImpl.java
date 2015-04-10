/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.taxonomy.api.Taxonomies;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TaxonomyManagerImpl implements TaxonomyManager {
    private static final long serialVersionUID = 1L;
    /**
     * Store cache for 10 users by default
     */
    public static final int DEFAULT_CACHE_SIZE = 10;
    /**
     * Cache expire time in seconds, 1 day by default
     */
    public static final int DEFAULT_EXPIRE_TIME = 60 * 60 * 24;

    private LoadingCache<String, Taxonomies> taxonomyCache;

    static Logger log = LoggerFactory.getLogger(TaxonomyManagerImpl.class);


    // injected by Spring
    private Repository repository;
    private Credentials credentials;
    private String taxonomiesContentPath;
    private int cacheSize = DEFAULT_CACHE_SIZE;
    private int expireTimeSeconds = DEFAULT_EXPIRE_TIME;


    @Override
    public Taxonomies getTaxonomies() {
        Session pluginSession = null;
        try {
            // try with context session, otherwise use provided one
            final Session contextSession = getContextSession();
            if (contextSession == null) {
                pluginSession = getPluginSession();
                return getTaxonomies(pluginSession);
            }
            return getTaxonomies(contextSession);

        } catch (RepositoryException e) {
            log.error("Error obtaining taxonomy session", e);
        } finally {
            if (pluginSession != null) {
                pluginSession.logout();
            }
        }

        return new NOOPTaxonomiesImpl();
    }


    private Taxonomies getTaxonomies(final Session session) {
        initCache();
        try {
            return taxonomyCache.get(getUserID(session), () -> buildTaxonomies(session));
        } catch (ExecutionException e) {
            log.error("Error populating taxonomy", e);
        }
        return new NOOPTaxonomiesImpl();
    }

    private void initCache() {
        if (taxonomyCache == null) {
            if (cacheSize < 0) {
                cacheSize = 0;
            }
            if (expireTimeSeconds < 0) {
                expireTimeSeconds = 0;
            }
            taxonomyCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(expireTimeSeconds, TimeUnit.SECONDS)
                    .maximumSize(cacheSize)
                    .build(new CacheLoader<String, Taxonomies>() {

                        @Override
                        public Taxonomies load(final String userName) throws Exception {
                            return null;
                        }
                    });
        }
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public String getTaxonomiesContentPath() {
        return this.taxonomiesContentPath;
    }

    public String setTaxonomiesContentPath(String taxonomiesContentPath) {
        return this.taxonomiesContentPath = taxonomiesContentPath;
    }

    private Taxonomies buildTaxonomies(final Session session) {
        if (taxonomiesContentPath == null || taxonomiesContentPath.isEmpty()) {
            log.warn("Cannot build taxonomies: taxonomiesContentPath is not configured");
            return new NOOPTaxonomiesImpl();
        }

        try {
            Node taxonomies = (Node)session.getItem(taxonomiesContentPath);

            if (taxonomies.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CONTAINER)) {
                log.debug("Loading  taxonomy for user: {}", getUserID(session));
                return new TaxonomiesImpl(taxonomies);
            } else {
                log.warn("Cannot build taxonomies: taxonomiesContentPath '{}' is not pointing to a node of type '{}'",
                        this.taxonomiesContentPath, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CONTAINER);
                return new NOOPTaxonomiesImpl();
            }
        } catch (PathNotFoundException e) {
            log.error("Unable to build taxonomies: taxonomiesContentPath '{}' does not resolve to node",
                    taxonomiesContentPath);
            return new NOOPTaxonomiesImpl();
        } catch (Exception e) {
            log.error("Unable to build taxonomies: {}", e);
            return new NOOPTaxonomiesImpl();
        }

    }


    public synchronized void invalidate(String path) {
        if (taxonomyCache != null) {
            // invalidate cache
            log.debug("Invalidating Taxonomy cache for path: {}", path);
            taxonomyCache.invalidateAll();
        }
    }

    private class NOOPTaxonomiesImpl implements Taxonomies {

        public List<Taxonomy> getRootTaxonomies() {
            return new ArrayList<>();
        }

        public Taxonomy getTaxonomy(String name) {
            return null;
        }

        public boolean isAggregating() {
            return false;
        }
    }


    private Session getContextSession() throws RepositoryException {
        final HstRequestContext context = RequestContextProvider.get();
        if (context != null) {
            return context.getSession();
        }
        return null;
    }


    private Session getPluginSession() throws RepositoryException {
        if (credentials == null) {
            throw new IllegalStateException("A valid credentials as well as repository should be set for TaxonomyManagerImpl.");
        }
        return this.repository.login(credentials);
    }

    private String getUserID(final Session session) {
        return session.getUserID();
    }

    public void setCacheSize(final int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setExpireTimeSeconds(final int expireTimeSeconds) {
        this.expireTimeSeconds = expireTimeSeconds;
    }
}
