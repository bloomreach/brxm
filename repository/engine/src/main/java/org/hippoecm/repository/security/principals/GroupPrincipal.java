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
package org.hippoecm.repository.security.principals;

import java.io.Serializable;
import java.security.Principal;

/**
 * The group principal holds a group of which a user is a member
 */
public class GroupPrincipal implements Principal, Serializable {

    /** SVN id placeholder */

    private static final long serialVersionUID = 1L;

    /**
     * The name of the role
     */
    private final String name;

    /**
     * The hash code
     */
    private transient int hash;

    /**
     * Creates a <code>GroupPrincipal</code> with the given name.
     *
     * @param name the name of this principal
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
     */
    public GroupPrincipal(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return ("GroupPrincipal: " + name);
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
        if (obj.getClass() == getClass()) {
            GroupPrincipal other = (GroupPrincipal) obj;
            return name.equals(other.name);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        if (hash == 0) {
            hash = name.hashCode();
        }
        return hash;
    }
}
