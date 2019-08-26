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
package org.hippoecm.repository.security.principals;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hippoecm.repository.security.domain.FacetAuthDomain;

/**
 * The facet auth principal holding all the {@link #getFacetAuthDomains() FacetAuthDomain}s for a user and their
 * overall aggregated {@link #getResolvedPrivileges() resolved privileges}
 */
public class FacetAuthPrincipal implements Principal {

    public static final FacetAuthPrincipal NO_AUTH_DOMAINS_PRINCIPAL = new FacetAuthPrincipal(Collections.EMPTY_SET);

    /**
     * The name representing this principal, effectively the class name
     */
    private final String name = FacetAuthPrincipal.class.getName();

    /**
     * The set of all UserDomains for the user
     * @see FacetAuthDomain
     */
    private final Set<FacetAuthDomain> facetAuthDomains;

    /**
     * The set of all the privileges of the user for all the domains with jcr:all and jcr:write replaced with their aggregate privileges
     */
    private final Set<String> resolvedPrivileges;

    /**
     * Creates a <code>FacetAuthPrincipal</code>.
     *
     * @param facetAuthDomains the FacetAuthDomains
     * @throws IllegalArgumentException if <code>facetAuthDomains</code> is <code>null</code>.
     */
    public FacetAuthPrincipal(final Set<FacetAuthDomain> facetAuthDomains) throws IllegalArgumentException {
        if (facetAuthDomains == null) {
            throw new IllegalArgumentException("facetAuthDomains can not be null");
        }
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
        return name;
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

    public String toString() {
        return name;
    }

    /**
     * Enforce that only a single instance of FacetAuthPrincipal can be contained within a set
     * @param obj
     * @return true when compared with any other FacetAuthPrincipal instance, else false
     */
    final public boolean equals(Object obj) {
        if (this == obj || obj instanceof FacetAuthPrincipal) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }
}
