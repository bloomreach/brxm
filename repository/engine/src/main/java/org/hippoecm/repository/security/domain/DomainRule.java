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

import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DomainRule holds a set of {@link QFacetRule}s that define a subset of the domain.
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

    private static final Logger log = LoggerFactory.getLogger(DomainRule.class);

    private Set<QFacetRule> facetRules = new HashSet<QFacetRule>();
    private String name;
    private String domainName;
    private transient int hash;

    public DomainRule(String name, String domainName, Set<QFacetRule> facetRules) {
        this.name = name;
        this.domainName = domainName;
        this.facetRules = facetRules;
    }

    /**
     * Instantiate the domain rule with the given configuration node. If
     * a QFacetRule of this DomainRule fails, the complete DomainRule must
     * fail to prevent widening the authorization on misconfiguration.
     * @param node the node folding the domain rule configuration
     * @throws RepositoryException
     */
    public DomainRule(Node node) throws RepositoryException {
        // loop over all the facet rules
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            if (child.getPrimaryNodeType().isNodeType(HippoNodeType.NT_FACETRULE)) {
                try {
                    facetRules.add(new QFacetRule(child));
                } catch (FacetRuleReferenceNotFoundException e) {
                    if (!e.isEquals()) {
                        // the facet rule has hipposys:equals = false, and thus can be skipped from the
                        // domain rule.
                        log.info("Skipping facet rule '{}' : {}", child.getPath(), e.getMessage());
                    } else {
                        // bubble up because hipposys:equals = true implying the domain rule can never result in a match
                        throw e;
                    }
                }
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
    public Set<QFacetRule> getFacetRules() {
        return Collections.unmodifiableSet(facetRules);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<QFacetRule> fr = getFacetRules();
        sb.append("DomainRule: ");
        sb.append("\r\n");
        for (QFacetRule rule : fr) {
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
