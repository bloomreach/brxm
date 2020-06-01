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
package org.onehippo.repository.security;

import java.util.TreeSet;
import javax.jcr.security.Privilege;

/**
 * <p>
 *     {@link DomainInfoPrivilege} is a simple wrapper class for a {@link Privilege} supporting extra information. It
 *     supports information about all the security domain paths ({@link #getDomainPaths()})
 *     that contributed to this specific {@link Privilege}. Because of how the security domains are set up, a single
 *     privilege on a {@link javax.jcr.Node}, eg jcr:read, can be the result of multiple security domains all giving
 *     jcr:read on the node. Hence {@link #getDomainPaths()} returns a {@link TreeSet} and not a single
 *     domain path (Treeset such that the domains paths are sorted alphabetically).
 * </p>
 */
public class DomainInfoPrivilege implements Privilege {

    private Privilege delegatee;

    private TreeSet<String> domainPaths = new TreeSet<>();

    public DomainInfoPrivilege(final Privilege delegatee) {
        if (delegatee == null) {
            throw new IllegalArgumentException("Delegatee privilege is not allowed to be null and the delegatee must have " +
                    "a non-null name");
        }
        this.delegatee = delegatee;
    }

    /**
     * @return All the security domain node paths providing this privilege, sorted alphabetically on the domain path
     */
    public TreeSet<String> getDomainPaths() {
        return domainPaths;
    }

    public void addDomainPath(final String domainPath) {
        domainPaths.add(domainPath);
    }

    @Override
    public String getName() {
        return delegatee.getName();
    }

    @Override
    public boolean isAbstract() {
        return delegatee.isAbstract();
    }

    @Override
    public boolean isAggregate() {
        return delegatee.isAggregate();
    }

    @Override
    public Privilege[] getDeclaredAggregatePrivileges() {
        return delegatee.getDeclaredAggregatePrivileges();
    }

    @Override
    public Privilege[] getAggregatePrivileges() {
        return delegatee.getAggregatePrivileges();
    }

}
