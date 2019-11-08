/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package com.bloomreach.xm.repository.security;

import java.util.SortedSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.onehippo.repository.security.SecurityConstants;

/**
 * Provides administrative (crud) domain management; currently limited to only {@link AuthRole}s of an existing domain.
 * <p>
 *     Note that this manager only provides and allows operations on {@link DomainAuth}s in a <em>valid</em> location!
 * </p>
 * <p>
 *     A domain location is valid if:
 * </p>
 * <ul>
 *     <li>it is a domain (directly) under a hipposys:domainfolder parent node below /hippo:configuration/hippo:domains</li>
 *     <li>it is a domain (directly) under a hipposys:federateddomainfolder parent node with depth >= 2</li>
 * </ul>
 * <p>
 *     Likewise, access and operations on {@link AuthRole}s is only provided for authroles directly under a <em>valid</em>
 *     domain location.
 * </p>
 * <p>
 *     All <em>modifying operations</em> require the underlying HippoSession to have userRole
 *     {@link SecurityConstants#USERROLE_SECURITY_APPLICATION_ADMIN}
 * </p>
 */
public interface DomainsManager {

    /**
     * Get a {@link DomainAuth} by its (node) path. Only a domain in a <em>valid</em> location will be returned.
     * @param domainPath the path of the domain to get
     * @return the DomainAuth object
     * @throws ItemNotFoundException when the domain node doesn't exist, or is in an invalid location
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    DomainAuth getDomainAuth(final String domainPath) throws ItemNotFoundException, RepositoryException;

    /**
     * @return all the {@link DomainAuth}s. Only DomainAuths in a <em>valid</em> location will be returned.
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    SortedSet<DomainAuth> getDomainAuths() throws RepositoryException;

    /**
     * @param user the name of the user
     * @return the {@link DomainAuth}s having at least one {@link AuthRole} with the provided user name.
     * Only domains in a <em>valid</em> location will be returned.
     * @throws IllegalArgumentException if the provided user name is null or invalid
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    SortedSet<DomainAuth> getDomainAuthsForUser(final String user) throws IllegalArgumentException, RepositoryException;

    /**
     * @param userRole the name of the userRole
     * @return the {@link DomainAuth}s having at least one {@link AuthRole} with the provided userRole.
     * Only domains in a <em>valid</em> location will be returned.
     * @throws IllegalArgumentException if the provided userRole name is null or invalid
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    SortedSet<DomainAuth> getDomainAuthsForUserRole(final String userRole) throws IllegalArgumentException, RepositoryException;

    /**
     * @param group the name of the group
     * @return the {@link DomainAuth}s having at least one {@link AuthRole} with the provided group.
     * Only domains in a <em>valid</em> location will be returned.
     * @throws IllegalArgumentException if the provided group name is null or invalid
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    SortedSet<DomainAuth> getDomainAuthsForGroup(final String group) throws IllegalArgumentException, RepositoryException;

    /**
     * Get a {@link AuthRole} by its (node) path. Only an authrole in a <em>valid</em> location will be returned.
     * @param authRolePath the path of the authrole to get
     * @return the AuthRole object
     * @throws ItemNotFoundException when the authrole node doesn't exist, or is in an invalid location
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    AuthRole getAuthRole(final String authRolePath) throws RepositoryException;

    /**
     * Add a new {@link AuthRole} to an existing {@link DomainAuth}.
     * @param authRoleTemplate A <em>template</em> for the AuthRole to create from, for example a {@link AuthRoleBean} instance
     * @return the created AuthRole
     * @throws IllegalArgumentException When the template, template.name, template.domainPath or template.role is null.
     * @throws ItemNotFoundException When there is no (valid) domain located at template.domainPath
     * @throws ItemExistsException When there already is an AuthRole named template.name under template.domainPath
     * @throws AccessDeniedException if the underlying HippoSession doesn't have the userRole
     * {@link SecurityConstants#USERROLE_SECURITY_APPLICATION_ADMIN}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    AuthRole addAuthRole(final AuthRole authRoleTemplate) throws IllegalArgumentException, AccessDeniedException, RepositoryException;

    /**
     * Update an existing {@link AuthRole}
     * @param authRoleTemplate A <em>template</em> for the AuthRole to update from, for example a {@link AuthRoleBean} instance
     * @return a fresh instance of the AuthRole after the update
     * @throws IllegalArgumentException When the template, template.name, template.domainPath or template.role is null.
     * @throws ItemNotFoundException When there is no (valid) domain located at template.domainPath
     * @throws AccessDeniedException if the underlying HippoSession doesn't have the userRole
     * {@link SecurityConstants#USERROLE_SECURITY_APPLICATION_ADMIN}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    AuthRole updateAuthRole(final AuthRole authRoleTemplate) throws IllegalArgumentException, AccessDeniedException, RepositoryException;

    /**
     * Delete an existing {@link AuthRole}
     * @param authRoleTemplate The AuthRole to delete
     * @return true if the AuthRole was deleted; false if not (not found or in an invalid location)
     * @throws IllegalArgumentException When the template or template.path is null.
     * @throws AccessDeniedException if the underlying HippoSession doesn't have the userRole
     * {@link SecurityConstants#USERROLE_SECURITY_APPLICATION_ADMIN}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    boolean deleteAuthRole(final AuthRole authRoleTemplate) throws IllegalArgumentException, AccessDeniedException, RepositoryException;
}
