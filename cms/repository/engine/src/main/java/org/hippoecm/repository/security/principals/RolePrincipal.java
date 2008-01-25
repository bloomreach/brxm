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

/**
 * The role principal holds a role assigned to a user or group
 */
public class RolePrincipal implements Principal, Serializable {

    private final String name;

    /**
     * Creates a <code>RolePrincipal</code> with the given name.
     *
     * @param name the name of this principal
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
     */
    public RolePrincipal(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }

    public String toString() {
        return ("RolePrincipal: " + name);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RolePrincipal) {
            RolePrincipal other = (RolePrincipal) obj;
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
}
