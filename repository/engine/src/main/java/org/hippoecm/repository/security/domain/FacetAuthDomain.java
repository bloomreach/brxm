/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Set;

/**
 * A user applicable {@link Domain} with all the active/expanded/resolved roles and privileges based on the matching
 * {@link AuthRole}(s) of the backing domain.
 */
public class FacetAuthDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The name representing the domain
     */
    private final String domainName;

    /**
     * The identifying path of the domain
     */
    private final String domainPath;

    /**
     * The set of domain rules for the domain
     * @see DomainRule
     */
    private final Set<DomainRule> rules;

    /**
     * The set of roles of the user for the domain
     */
    private final Set<String> roles;

    /**
     * The set of privileges of the user for the domain
     */
    private final Set<String> privileges;

    /**
     * The set of privileges of the user for the domain with jcr:all and jcr:write replaced with their aggregate privileges
     */
    private final Set<String> resolvedPrivileges;

    public FacetAuthDomain(final String domainName, final String domainPath, final Set<DomainRule> domainRules,
                           final Set<String> roles, final Set<String> privileges, Set<String> resolvedPrivileges)
            throws IllegalArgumentException {
        if (domainName == null) {
            throw new IllegalArgumentException("domainName can not be null");
        }
        if (domainPath == null) {
            throw new IllegalArgumentException("domainPath can not be null");
        }
        if (domainRules == null){
            throw new IllegalArgumentException("domainRules can not be null");
        }
        if (domainRules.size() == 0) {
            throw new IllegalArgumentException("domainRules must contain at least one value");
        }
        if (roles == null){
            throw new IllegalArgumentException("roles can not be null");
        }
        if (privileges == null){
            throw new IllegalArgumentException("privileges can not be null");
        }
        if (resolvedPrivileges == null){
            throw new IllegalArgumentException("resolvedPrivileges can not be null");
        }

        // assigning values
        this.domainName = domainName;
        this.domainPath = domainPath;
        this.roles = Collections.unmodifiableSet(roles);
        this.rules = Collections.unmodifiableSet(domainRules);
        this.privileges = Collections.unmodifiableSet(privileges);
        this.resolvedPrivileges = Collections.unmodifiableSet(resolvedPrivileges);
    }

    /**
     * Get the name of the domain
     * @return the name of the domain
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Get the identifying path of the domain
     * @return the path of the domain
     */
    public String getDomainPath() {
        return domainPath;
    }

    /**
     * Get the set of roles
     * @return the roles the user has for the domain
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Get the set of privileges
     * @return the privileges the user has for the domain
     */
    public Set<String> getPrivileges() {
        return privileges;
    }

    /**
     * Get the set of resolved privileges with jcr:all and jcr:write replaced with their aggregate privileges
     * @return the resolvedPrivileges the user has for the domain
     */
    public Set<String> getResolvedPrivileges() {
        return resolvedPrivileges;
    }

    /**
     * Get the set of domain rules defining the domain
     * @return the domain rules defining the domain
     */
    public Set<DomainRule> getRules() {
        return rules;
    }


    public String toString() {
        return ("UserDomain: " + domainPath);
    }

    public boolean equals(Object obj) {
        return obj instanceof FacetAuthDomain && domainPath.equals(((FacetAuthDomain)obj).getDomainPath());
    }

    public int hashCode() {
        return domainPath.hashCode();
    }
}
