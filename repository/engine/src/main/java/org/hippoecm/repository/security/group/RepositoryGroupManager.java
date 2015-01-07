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
package org.hippoecm.repository.security.group;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;

/**
 * The GroupManager that stores the groups in the JCR Repository
 */
public class RepositoryGroupManager extends AbstractGroupManager {

    @Override
    public void initManager(ManagerContext context) throws RepositoryException {
        initialized = true;
    }

    /**
     * The backend is the repository, so just return the current memberships
     */
    @Override
    public Set<String> backendGetMemberships(Node user) throws RepositoryException {
        return getMembershipIds(user.getName());
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Override
    public String getNodeType() {
        return HippoNodeType.NT_GROUP;
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

}
