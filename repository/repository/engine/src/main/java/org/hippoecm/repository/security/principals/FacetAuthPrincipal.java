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

import org.apache.jackrabbit.spi.Name;

public class FacetAuthPrincipal implements Serializable, Principal {

    private static final long serialVersionUID = 1L;
    private final String name;
    private final Name facet;
    private final String[] values;
    private final long permissions;

    /**
     * Creates a <code>UserPrincipal</code> with the given name.
     *
     * @param facet
     * @param values
     * @param permissionss
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
     */
    public FacetAuthPrincipal(Name facet, String[] values, long permissions) throws IllegalArgumentException {
        if (facet == null) {
            throw new IllegalArgumentException("facet can not be null");
        }
        if (values == null){
            throw new IllegalArgumentException("values can not be null");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("values must contain at least one values");
        }


        // adding values
        this.facet = facet;
        this.values = values.clone();
        this.name = buildString(facet, values, permissions);
        this.permissions = permissions;
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

    //------------------------------------------------------------< Principal >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    public Name getFacet() {
        return facet;
    }

    public String[] getValues() {
        return values.clone();
    }

    public long getPermissions() {
        return permissions;
    }
}
