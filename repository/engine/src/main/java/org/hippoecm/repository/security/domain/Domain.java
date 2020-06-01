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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Domain holds a set of {@link DomainRule}s that define the domain.
 * The DomainRule must be merged together with ORs. The domain also holds
 * the {@link AuthRole}s which define which users and group has a given
 * role in the domain.
 *
 * In JCR:
 * Domain
 * + DomainRule1
 * +-- FacetRule1
 * +-- FacetRule2
 * +-- FacetRule3
 * + DomainRule2
 * +-- FacetRule4
 * +-- FacetRule5
 * +-- FacetRule6
 * + AuthRole1
 * + AuthRole2
 */
public class Domain {

    /** SVN id placeholder */

    /**
     * The name of the domain
     */
    private final String name;

    /**
     * The set of domain rules defining the domain
     * @see DomainRule
     */
    private Set<DomainRule> domainRules = new HashSet<DomainRule>();

    /**
     * The set of auth roles defining who has which role in the domain
     * @see AuthRole
     */
    private Set<AuthRole> authRoles = new HashSet<AuthRole>();

    /**
     * The hash code
     */
    private transient int hash;

    /**
     * Logger
     */
    static final Logger log = LoggerFactory.getLogger(Domain.class);

    /**
     * Initialize the Domain from the node in the repository. On initialization
     * all domain rules and including facet rules as wel as all auth roles are
     * created from the JCR configuration
     * @param node the node holding the configuration
     * @throws RepositoryException
     */
    public Domain(final Node node) throws RepositoryException {
        if (node == null) {
            throw new IllegalArgumentException("Domain node cannot be null");
        }
        name = node.getName();

        // loop over all the facetrules and authroles
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            try {
                if (child.getPrimaryNodeType().isNodeType(HippoNodeType.NT_DOMAINRULE)) {
                    try {
                        domainRules.add(new DomainRule(child));
                    } catch (FacetRuleReferenceNotFoundException e){
                        log.info("Skipping domain rule '{}' because {}", child.getPath(), e.getMessage());
                    }
                } else if (child.getPrimaryNodeType().isNodeType(HippoNodeType.NT_AUTHROLE)) {
                    authRoles.add(new AuthRole(child));
                } else {
                    log.warn("Unknown domain config node '{}' found in '{}' ", child.getName(), node.getPath());
                }
            } catch (RepositoryException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Unable to add DomainRule '{}'", child.getPath(), e);
                } else {
                    log.warn("Unable to add DomainRule '{}' : {}", child.getPath(), e.getMessage());
                }
            }
        }
    }

    /**
     * Get the set of domain rules that define the domain
     * @return the set of domain rules
     */
    public Set<DomainRule> getDomainRules() {
        return Collections.unmodifiableSet(domainRules);
    }

    /**
     * Get the set of auth roles belonging to the domain
     * @return the set of auth roles
     */
    public Set<AuthRole> getAuthRoles() {
        return Collections.unmodifiableSet(authRoles);
    }

    /**
     * Get the roles the user with userId has in the domain
     * @param userId the id of the user
     * @return the roles the user has in the domain
     */
    public Set<String> getRolesForUser(String userId) {
        Set<AuthRole> ars = getAuthRoles();
        Set<String> roles = new HashSet<String>();
        for (AuthRole ar : ars) {
            if (ar.hasUser(userId)) {
                roles.add(ar.getRole());
            }
        }
        return roles;
    }

    /**
     * Get the roles the group with groupId has in the domain
     * @param groupId the id of the group
     * @return the roles the groups has in the domain
     */
    public Set<String> getRolesForGroup(String groupId) {
        Set<AuthRole> ars = getAuthRoles();
        Set<String> roles = new HashSet<String>();
        for (AuthRole ar : ars) {
            if (ar.hasGroup(groupId)) {
                roles.add(ar.getRole());
            }
        }
        return roles;
    }

    /**
     * Get the name of the domain
     * @return the domain name
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        Set<DomainRule> drs = getDomainRules();
        Set<AuthRole> ars = getAuthRoles();
        sb.append("Domain: ");
        sb.append(name);
        sb.append("\r\n");
        sb.append("-------");
        sb.append("\r\n");
        for (DomainRule dr : drs) {
            sb.append(dr.toString());
        }
        for (AuthRole ar : ars) {
            sb.append(ar.toString());
        }
        sb.append("-------");
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
        if (!(obj instanceof Domain)) {
            return false;
        }
        Domain other = (Domain) obj;
        if (!name.equals(other.getName())) {
            return false;
        }
        if (authRoles.size() != other.getAuthRoles().size() || !authRoles.containsAll(other.getAuthRoles())) {
            return false;
        }
        if (domainRules.size() != other.getDomainRules().size() || !domainRules.containsAll(other.getDomainRules())) {
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
