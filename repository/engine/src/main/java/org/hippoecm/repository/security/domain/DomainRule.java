/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.NodeNameCodec;
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

    private final Set<QFacetRule> facetRules;
    private final String name;
    private final String domainName;

    /**
     * Instantiate a domain rule based on another domain rule with extra facet rules
     * @param other another domain rule
     * @param extensionFacetRules extra facet rules
     */
    public DomainRule(DomainRule other, Set<QFacetRule> extensionFacetRules) {
        HashSet<QFacetRule> combinedFacetRules = new HashSet<>(other.getFacetRules());
        combinedFacetRules.addAll(extensionFacetRules);
        facetRules = Collections.unmodifiableSet(combinedFacetRules);
        name = other.getName();
        domainName = other.getDomainName();
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
        HashSet<QFacetRule> collectedFacetRules = new HashSet<>();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            if (child.isNodeType(HippoNodeType.NT_FACETRULE)) {
                collectedFacetRules.add(new QFacetRule(child));
            }
        }
        facetRules = Collections.unmodifiableSet(collectedFacetRules);
        this.name = NodeNameCodec.decode(node.getName());
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
        return facetRules;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<QFacetRule> fr = getFacetRules();
        sb.append("DomainRule: ");
        sb.append(name);
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
        return obj instanceof DomainRule && getName().equals(((DomainRule)obj).getName());
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return name.hashCode();
    }
}
