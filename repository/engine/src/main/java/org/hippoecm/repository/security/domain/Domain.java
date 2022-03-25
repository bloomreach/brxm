/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_FEDERATEDDOMAINFOLDER;
import static org.onehippo.repository.security.SecurityConstants.CONFIGURATION_FOLDER_PATH;
import static org.onehippo.repository.security.SecurityConstants.CONFIGURATION_FOLDER_PATH_PREFIX;

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
     * The name of the domain
     */
    private final String name;

    /**
     * The identifying path of the domain
     */
    private final String path;

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
     * Checks if the provided domain folder node its primary type is hipposys:domainfolder and
     * in that case is not directly below /hippo:configuration.
     * @return true if the domain folder node its primary type is hipposys:domainfolder and is not directly below
     * /hippo:configuration
     * @throws RepositoryException if something goes wrong
     */
    public static boolean isInValidStandardDomainFolderLocation(final Node domainFolder)
            throws RepositoryException {
        return HippoNodeType.NT_DOMAINFOLDER.equals(domainFolder.getPrimaryNodeType().getName()) &&
                (domainFolder.getDepth() != 2 ||
                        !CONFIGURATION_FOLDER_PATH.equals(domainFolder.getParent().getPath()));
    }

    /**
     * Checks if the provided domain folder node is of type hipposys:federateddomainfolder and
     * in that case is not directly below the jcr:root and neither below /hippo:configuration
     * @return true if the domain folder node is of type hipposys:federateddomainfolder and is not directly below
     * the jcr:root and neither below /hippo:configuration
     * @throws RepositoryException if something goes wrong
     */
    public static boolean isInValidFederatedDomainFolderLocation(final Node domainFolder)
            throws RepositoryException {
        return domainFolder.isNodeType(HippoNodeType.NT_FEDERATEDDOMAINFOLDER) &&
                (domainFolder.getDepth() == 1 || domainFolder.getPath().startsWith(CONFIGURATION_FOLDER_PATH_PREFIX));
    }

    /**
     * Checks if the provided domain folder is in a valid location, if both
     * {@link #isInValidStandardDomainFolderLocation(Node)} and {@link #isInValidFederatedDomainFolderLocation(Node)}
     * returning false
     * @param domainFolder domainFolder
     * @return true if domainFolder is in a valid location
     * @throws RepositoryException if something is wrong
     */
    public static boolean isValidDomainFolderLocation(final Node domainFolder) throws RepositoryException {
        return !isInValidStandardDomainFolderLocation(domainFolder) && !isInValidFederatedDomainFolderLocation(domainFolder);
    }

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
        path = node.getPath();

        HashSet<DomainRule> domainRules = new HashSet<>();
        HashSet<AuthRole> authRoles = new HashSet<>();

        // loop over all the facetrules and authroles
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            try {
                if (child.isNodeType(HippoNodeType.NT_DOMAINRULE)) {
                    domainRules.add(new DomainRule(child));
                } else if (child.isNodeType(HippoNodeType.NT_AUTHROLE)) {
                    authRoles.add(new AuthRole(child));
                } else {
                    log.warn("Unknown domain config node '{}' found in '{}' ", child.getName(), node.getPath());
                }
            } catch (RepositoryException e) {
                // Other domain rules can still be valid for the domain
                if (log.isDebugEnabled()) {
                    log.warn("Unable to add DomainRule '{}'", child.getPath(), e);
                } else {
                    log.warn("Unable to add DomainRule '{}' : {}", child.getPath(), e.getMessage());
                }
            } catch (InvalidDomainException e) {
                log.error("Skipping invalid domain rule '{}': {}", child.getPath(), e.getMessage());
            }
        }

        if (domainRules.isEmpty()) {
            throw new InvalidDomainException(String.format("Invalid domain '%s': a domain requires at least one valid domain rule", node.getPath()));
        }
        if (node.getParent().isNodeType(NT_FEDERATEDDOMAINFOLDER)) {

            // then every domain below the federateddomainfolder MUST have at least one hierarchical allowed constraint
            for (DomainRule domainRule : domainRules) {
                boolean validFederatedDomain = domainRule.getFacetRules().stream().anyMatch(QFacetRule::isHierarchicalAllowlistRule);
                if (!validFederatedDomain) {
                    throw new InvalidDomainException(String.format("Invalid federated domain '%s' since every " +
                                    "domain in a federated domain requires at least 1 hierchical constrained confining the " +
                                    "domain to nodes which are children of the parent of the hipposys:federateddomainfolder",
                            node.getPath()));
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
        return getAuthRoles().stream()
                .filter(ar -> ar.hasUser(userId))
                .map(AuthRole::getRole)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Get the roles the group with groupId has in the domain
     * @param groupId the id of the group
     * @return the roles the groups has in the domain
     */
    public Set<String> getRolesForGroup(String groupId) {
        return getAuthRoles().stream()
                .filter(ar -> ar.hasGroup(groupId))
                .map(AuthRole::getRole)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Get the roles the user role with userRoleId has in the domain
     * @param userRoleId the id of the user role
     * @return the roles the user role has in the domain
     */
    public Set<String> getRolesForUserRole(String userRoleId) {
        return getAuthRoles().stream()
                .filter(ar -> userRoleId.equals(ar.getUserRole()))
                .map(AuthRole::getRole)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Get the name of the domain, unique within its domain folder
     * @return the domain name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the identifying path of the domain
     * @return the identifying domain path
     */
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        Set<DomainRule> drs = getDomainRules();
        Set<AuthRole> ars = getAuthRoles();
        sb.append("Domain: ");
        sb.append(path);
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
        return obj instanceof Domain && getPath().equals(((Domain)obj).getPath());
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return getPath().hashCode();
    }
}
