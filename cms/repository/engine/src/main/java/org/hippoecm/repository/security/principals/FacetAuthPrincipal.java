/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security.principals;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.security.domain.DomainRule;

/**
 * The facet auth principal holding the domain rules, roles and
 * JCR permissions for a domain
 */
public class FacetAuthPrincipal implements Serializable, Principal {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    /**
     * Serial verion id
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * The name representing the domain
     */
    private final String name;
    
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
     * The JCR permissions of the user for the domain
     */
    private final int permissions;

    
    /**
     * Creates a <code>UserPrincipal</code> with the given name.
     *
     * @param facet
     * @param values
     * @param permissionss
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
     */
    public FacetAuthPrincipal(String domain, Set<DomainRule> domainRules, Set<String> roles, int permissions) throws IllegalArgumentException {
        if (domain == null) {
            throw new IllegalArgumentException("facet can not be null");
        }
        if (domainRules == null){
            throw new IllegalArgumentException("values can not be null");
        }
        if (domainRules.size() == 0) {
            throw new IllegalArgumentException("values must contain at least one values");
        }
        if (roles == null){
            throw new IllegalArgumentException("roles can not be null");
        }

        // assigning values
        this.name = domain;
        this.roles = Collections.unmodifiableSet(roles);
        this.rules = Collections.unmodifiableSet(domainRules);
        this.permissions = permissions;
    }

  


    //------------------------------------------------------------< Principal >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * Get the set of roles
     * @return the roles the user has for the domain
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Get the set of domain rules defining the domain
     * @return the domain rules defining the domain
     */
    public Set<DomainRule> getRules() {
        return rules;
    }

    /**
     * Get the JCR permissions
     * @return the JCR permissions the user has for the domain
     */
    public int getPermissions() {
        return permissions;
    }
    
    
    
    
    
    
    /**
     * String for pretty printing and generating the hashcode
     * @param facet
     * @param values
     * @param permissions
     * @return
     */
    private String buildString(Name facet, String[] values, long permissions) {
        // create nice name (also used for hashing)
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        buf.append(facet);
        buf.append(" = ");
        for(int i = 0; i<values.length;i++) {
            if (i > 0) buf.append(" or ");
            buf.append(values[i]);
        }
        buf.append("] [");
        buf.append("permissions: ");
        buf.append(permissions);
        buf.append("]");
        return buf.toString();
    }

    public String toString() {
        return ("FacetAuthPrincipal: " + name);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FacetAuthPrincipal) {
            FacetAuthPrincipal other = (FacetAuthPrincipal) obj;
            return name.equals(other.name);
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }
}
