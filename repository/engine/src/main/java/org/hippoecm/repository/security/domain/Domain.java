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
 * the {@link AuthRole}s which define which users and/or groups and/or user role has a given
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

    /**
     * The name of the domain, unique within its domain folder
     */
    private final String name;

    /**
     * The set of domain rules defining the domain
     * @see DomainRule
     */
    private final Set<DomainRule> domainRules;

    /**
     * The set of auth roles defining who has which role in the domain
     * @see AuthRole
     */
    private final Set<AuthRole> authRoles;

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(Domain.class);

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

        HashSet<DomainRule> domainRules = new HashSet<>();
        HashSet<AuthRole> authRoles = new HashSet<>();

        // loop over all the facetrules and authroles
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            try {
                if (child.getPrimaryNodeType().isNodeType(HippoNodeType.NT_DOMAINRULE)) {
                    domainRules.add(new DomainRule(child));
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
        this.domainRules = Collections.unmodifiableSet(domainRules);
        this.authRoles = Collections.unmodifiableSet(authRoles);
    }

    /**
     * Get the set of domain rules that define the domain
     * @return the set of domain rules
     */
    public Set<DomainRule> getDomainRules() {
        return domainRules;
    }

    /**
     * Get the set of auth roles belonging to the domain
     * @return the set of auth roles
     */
    public Set<AuthRole> getAuthRoles() {
        return authRoles;
    }

    /**
     * Get the roles the user with userId has in the domain
     * @param userId the id of the user
     * @return the roles the user has in the domain
     */
    public Set<String> getRolesForUser(String userId) {
        Set<AuthRole> ars = getAuthRoles();
        Set<String> roles = new HashSet<>();
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
        Set<String> roles = new HashSet<>();
        for (AuthRole ar : ars) {
            if (ar.hasGroup(groupId)) {
                roles.add(ar.getRole());
            }
        }
        return roles;
    }

    /**
     * Get the roles the user role with userRoleId has in the domain
     * @param userRoleId the id of the user role
     * @return the roles the user role has in the domain
     */
    public Set<String> getRolesForUserRole(String userRoleId) {
        Set<AuthRole> ars = getAuthRoles();
        Set<String> roles = new HashSet<>();
        for (AuthRole ar : ars) {
            if (userRoleId.equals(ar.getUserRole())) {
                roles.add(ar.getRole());
            }
        }
        return roles;
    }

    /**
     * Get the name of the domain, unique within its domain folder
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
        return obj instanceof Domain && getName().equals(((Domain)obj).getName());
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return getName().hashCode();
    }
}
