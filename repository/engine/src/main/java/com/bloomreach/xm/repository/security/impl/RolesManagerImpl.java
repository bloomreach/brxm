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

import java.util.Objects;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.Role;
import com.bloomreach.xm.repository.security.RolesManager;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PRIVILEGES;

class RolesManagerImpl extends AbstractRolesManager<Role> implements RolesManager {

    private static final Logger log = LoggerFactory.getLogger(RolesManager.class);

    RolesManagerImpl(final RepositorySecurityManagerImpl repositorySecurityManager) {
        super(repositorySecurityManager, (RolesProviderImpl)repositorySecurityManager.getRolesProvider(), log);
    }

    @Override
    protected boolean updateRoleNode(final Role roleTemplate, final Node roleNode)
            throws RepositoryException {
        boolean updated = super.updateRoleNode(roleTemplate, roleNode);
        final Set<String> nodePrivileges = JcrUtils.getStringSetProperty(roleNode, HIPPO_PRIVILEGES, null);
        if (!Objects.equals(roleTemplate.getPrivileges(), nodePrivileges)) {
            if (roleTemplate.getPrivileges().isEmpty()) {
                roleNode.setProperty(HIPPO_PRIVILEGES, (String[])null);
            } else {
                roleNode.setProperty(HIPPO_PRIVILEGES, roleTemplate.getPrivileges().toArray(new String[0]));
            }
            updated = true;
        }
        return updated;
    }
}
