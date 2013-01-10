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
 * The admin principal gives read, write and remove privileges
 * to the whole repository except from the jcr namespace nodes
 * and attributes, which require SystemPrincipal.
 */
@Deprecated
public class AdminPrincipal implements Principal, Serializable {

    /** SVN id placeholder */

    private static final long serialVersionUID = 1L;

    private static final String ADMIN_USER = "admin";

    /**
     * Creates an <code>AdminPrincipal</code>.
     */
    public AdminPrincipal() {
    }

    public String toString() {
        return "AdminPrincipal";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AdminPrincipal) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return ADMIN_USER.hashCode();
    }

    //------------------------------------------------------------< Principal >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return ADMIN_USER;
    }
}
