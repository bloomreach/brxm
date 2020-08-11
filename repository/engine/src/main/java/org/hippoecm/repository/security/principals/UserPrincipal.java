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
package org.hippoecm.repository.security.principals;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hippoecm.repository.security.domain.FacetAuthDomain;
import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.security.User;

/**
 * A principal wrapping a {@link SessionUser} with {@link #getName()} delegated to {@link User#getId()}, and holding all
 * the {@link #getFacetAuthDomains() FacetAuthDomain}s for a user and their overall aggregated
 * {@link #getResolvedPrivileges() resolved privileges}.
 * <p>
 * A UserPrincipal compares equal to any other instance of UserPrincipal: only one instance can be contained
 * in a set.
 * </p>
 * <p>
 * The User can be retrieved through {@link #getUser()}.
 * </p>
 */
public final class UserPrincipal implements Principal {

    /**
     * The user of this principal
     */
    private final SessionUser user;

    /**
     * The set of all UserDomains for the user
     * @see FacetAuthDomain
     */
    private final Set<FacetAuthDomain> facetAuthDomains;

    /**
     * The set of all the privileges of the user for all the domains with jcr:all and jcr:write replaced with their aggregate privileges
     */
    private final Set<String> resolvedPrivileges;

    public UserPrincipal(final SessionUser user, final Set<FacetAuthDomain> facetAuthDomains) throws IllegalArgumentException {
        if (user == null) {
            throw new IllegalArgumentException("user can not be null");
        }
        if (facetAuthDomains == null) {
            throw new IllegalArgumentException("facetAuthDomains can not be null");
        }
        this.user = user;
        this.facetAuthDomains = Collections.unmodifiableSet(facetAuthDomains);
        HashSet<String> resolvedPrivileges = new HashSet<>();
        facetAuthDomains.forEach(p -> resolvedPrivileges.addAll(p.getResolvedPrivileges()));
        this.resolvedPrivileges = Collections.unmodifiableSet(resolvedPrivileges);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return user.getId();
    }

    public SessionUser getUser() {
        return user;
    }

    /**
     * Get all {@link FacetAuthDomain}s for the user
     * @return all FacetAuthDomains for the user
     */
    public Set<FacetAuthDomain> getFacetAuthDomains() {
        return facetAuthDomains;
    }

    /**
     * Get the set of all resolved privileges for the user with jcr:all and jcr:write replaced with their aggregate privileges
     * @return the resolvedPrivileges for the user
     */
    public Set<String> getResolvedPrivileges() {
        return resolvedPrivileges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ("UserPrincipal: " + getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof UserPrincipal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
