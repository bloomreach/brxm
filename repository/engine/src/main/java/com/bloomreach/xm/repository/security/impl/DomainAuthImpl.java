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
package com.bloomreach.xm.repository.security.impl;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.DomainAuth;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DESCRIPTION;
import static org.hippoecm.repository.api.HippoNodeType.NT_AUTHROLE;

public class DomainAuthImpl implements DomainAuth {

    private final String name;
    private final String path;
    private final String folderPath;
    private final String description;
    private final SortedMap<String, AuthRole> authRolesMap;

    public DomainAuthImpl(final Node domainNode) throws RepositoryException {
        this.name = NodeNameCodec.decode(domainNode.getName());
        this.path = domainNode.getPath();
        this.folderPath = domainNode.getParent().getPath();
        this.description = JcrUtils.getStringProperty(domainNode, HIPPOSYS_DESCRIPTION, null);
        final TreeMap<String, AuthRole> authRoleMap = new TreeMap<>();
        final NodeIterator domainChildrenIter = domainNode.getNodes();
        while (domainChildrenIter.hasNext()) {
            final Node childNode = domainChildrenIter.nextNode();
            if (childNode.isNodeType(NT_AUTHROLE)) {
                final AuthRole authRole = new AuthRoleImpl(childNode);
                authRoleMap.put(authRole.getName(), authRole);
            }
        }
        this.authRolesMap = Collections.unmodifiableSortedMap(authRoleMap);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getFolderPath() {
        return folderPath;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public SortedMap<String, AuthRole> getAuthRolesMap() {
        return authRolesMap;
    }

    @Override
    public AuthRole getAuthRole(final String authRoleName) {
        return authRolesMap.get(authRoleName);
    }

    @Override
    public int compareTo(final DomainAuth o) {
        return o.getPath().compareTo(path);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DomainAuth && path.equals(((DomainAuth)obj).getPath());
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
