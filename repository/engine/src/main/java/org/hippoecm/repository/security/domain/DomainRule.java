/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;

/**
 * A DomainRule holds a set of {@link FacetRule}s that define a subset of the domain.
 * The FacetRules must be merged together with ANDs.
 *
 * In JCR:
 * - DomainRule
 * +-- FacetRule1
 * +-- FacetRule2
 * +-- FacetRule3
 */
public class DomainRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Set<FacetRule> facetRules = new HashSet<FacetRule>();
    private String name;
    private String domainName;
    private transient int hash;

    public DomainRule(String name, String domainName, Set<FacetRule> facetRules) {
        this.name = name;
        this.domainName = domainName;
        this.facetRules = facetRules;
    }

    /**
     * Instantiate the domain rule with the given configuration node. If
     * a FacetRule of this DomainRule fails, the complete DomainRule must
     * fail to prevent widening the authorization on misconfiguration.
     * @param node the node folding the domain rule configuration
     * @throws RepositoryException
     */
    public DomainRule(Node node) throws RepositoryException {
        if (node == null) {
            throw new IllegalArgumentException("DomainRule node cannot be null");
        }
        // loop over all the facet rules
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            if (child.getPrimaryNodeType().isNodeType(HippoNodeType.NT_FACETRULE)) {
                facetRules.add(new FacetRule(child));
            }
        }
        this.name = node.getName();
        this.domainName = node.getParent().getName();
    }

    /**
     * Get the name of this DomainRule.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the domain this DomainRule is part of.
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Get the facet rules defining the domain rule
     */
    public Set<FacetRule> getFacetRules() {
        return Collections.unmodifiableSet(facetRules);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<FacetRule> fr = getFacetRules();
        sb.append("DomainRule: ");
        sb.append("\r\n");
        for (FacetRule rule : fr) {
            sb.append("  ");
            sb.append(rule.toString());
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DomainRule)) {
            return false;
        }
        DomainRule other = (DomainRule) obj;
        if (facetRules.size() != other.getFacetRules().size() || !facetRules.containsAll(other.getFacetRules())) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        if (hash == 0) {
            hash = this.toString().hashCode();
        }
        return hash;
    }
}
