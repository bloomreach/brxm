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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.taxonomy.api.Taxonomies;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO see if the taxonomy manager can be made context aware in case of preview/live when having counts implemented
public class TaxonomyManagerImpl implements TaxonomyManager {
    private static final long serialVersionUID = 1L;

    static Logger log = LoggerFactory.getLogger(TaxonomyManagerImpl.class);
    private Taxonomies taxonomies;

    // injected by Spring
    private Repository repository;
    private Credentials credentials;
    private String taxonomiesContentPath;

    public TaxonomyManagerImpl() {
    }

    public Taxonomies getTaxonomies() {
        Taxonomies tax = this.taxonomies;
        if (tax == null) {
            long start = System.currentTimeMillis();
            synchronized (this) {
                buildTaxonomies();
                tax = this.taxonomies;
            }
            log.info("Building taxonomy tree took " + (System.currentTimeMillis() - start) + " ms.");
        }
        return tax;
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

    private synchronized void buildTaxonomies() {
        if (this.taxonomies != null) {
            return;
        }
        if (taxonomiesContentPath == null || "".equals(taxonomiesContentPath)) {
            log.warn("Cannot build taxonomies: taxonomiesContentPath is not configured");
            this.taxonomies = new NOOPTaxonomiesImpl();
            return;
        }

        try {
            Node taxonomies = getRootNode(taxonomiesContentPath);
            if (taxonomies.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CONTAINER)) {
                this.taxonomies = new TaxonomiesImpl(taxonomies);
            } else {
                log.warn("Cannot build taxonomies: taxonomiesContentPath '{}' is not pointing to a node of type '{}'",
                        this.taxonomiesContentPath, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CONTAINER);
                this.taxonomies = new NOOPTaxonomiesImpl();
                return;
            }
        } catch (PathNotFoundException e) {
            log.error("Unable to build taxonomies: taxonomiesContentPath '{}' does not resolve to node",
                    taxonomiesContentPath);
            this.taxonomies = new NOOPTaxonomiesImpl();
        } catch (Exception e) {
            log.error("Unable to build taxonomies: {}", e);
            this.taxonomies = new NOOPTaxonomiesImpl();
        }

    }

    protected Node getRootNode(String taxonomiesContentPath) throws LoginException, RepositoryException {
        if (credentials == null) {
            throw new IllegalStateException("A valid credentials as well as repository should be set for TaxonomyManagerImpl.");
        }

        Session session = this.repository.login(credentials);
        return (Node) session.getItem(taxonomiesContentPath);
    }

    public synchronized void invalidate(String path) {
        this.taxonomies = null;
    }

    private class NOOPTaxonomiesImpl implements Taxonomies {

        public List<Taxonomy> getRootTaxonomies() {
            return new ArrayList<Taxonomy>();
        }

        public Taxonomy getTaxonomy(String name) {
            return null;
        }

        public boolean isAggregating() {
            return false;
        }
    }
}
