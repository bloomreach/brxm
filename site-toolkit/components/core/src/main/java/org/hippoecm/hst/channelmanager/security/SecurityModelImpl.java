/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.channelmanager.security;


import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityModelImpl implements SecurityModel {

    private static final Logger log = LoggerFactory.getLogger(SecurityModelImpl.class);

    private Repository repository;
    private Credentials credentials;
    private String rootPath;
    private String jcrPathTemplateComposer;

    // mapping for [ role --> jcr {privilege, privilegePath} mapping ]
    private volatile Map<String, PrivilegePathMapping> mappingModel;

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    public void setJcrPathTemplateComposer(final String jcrPathTemplateComposer) {
        this.jcrPathTemplateComposer = jcrPathTemplateComposer;
    }

    @Override
    public Principal getUserPrincipal(final Session session) {
        return session::getUserID;
    }


    /**
     * Below might be quite an odd implementation but this is because of legacy reasons how before it was found out
     * whether a user was an admin or webmaster. Hence this kind of awkward looking implementation
     */
    @Override
    public boolean isUserInRole(final Session session, final String functionalRole) {

        if (CHANNEL_MANAGER_ADMIN_ROLE.equals(functionalRole)) {
            final Map<String, PrivilegePathMapping> mapping = getMappingModel();
            final PrivilegePathMapping privilegePathMappging = mapping.get(functionalRole);
            if (privilegePathMappging == null) {
                log.info("No PrivilegePathMapping for role '{}'.", functionalRole);
                return false;
            }
            try {
                return session.hasPermission(privilegePathMappging.privilegePath, privilegePathMappging.privilege);
            } catch (RepositoryException e) {
                throw new IllegalStateException("Exception while checking permissions.", e);
            }
        } else if (CHANNEL_WEBMASTER_ROLE.equals(functionalRole)) {
            try {
                return session.hasPermission(rootPath + "/accesstest", Session.ACTION_SET_PROPERTY);
            } catch (RepositoryException e) {
                log.warn("Could not determine authorization", e);
                throw new IllegalStateException("Exception while checking permissions.", e);
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported Functional role '%s'.", functionalRole));
    }

    private Map<String, PrivilegePathMapping> getMappingModel() {
        Map<String, PrivilegePathMapping> mapping = mappingModel;
        if (mapping != null) {
            return mapping;
        }
        synchronized (this) {
            mapping = mappingModel;
            if (mapping != null) {
                return mapping;
            }

            mapping = new HashMap<>();
            mappingModel = mapping;
            Session session = null;
            try {
                session = repository.login(credentials);
                final Node templateComposerNode = JcrUtils.getNodeIfExists(jcrPathTemplateComposer, session);
                if (templateComposerNode == null) {
                    log.warn("Missing jcr node at '{}' to read required configured admin privileges from: Return empty SecurityModel.",
                            jcrPathTemplateComposer);
                    return mapping;
                }
                final String manageChangesPrivileges = JcrUtils.getStringProperty(templateComposerNode, "manage.changes.privileges", null);
                final String manageChangesPrivilegesPath = JcrUtils.getStringProperty(templateComposerNode, "manage.changes.privileges.path", null);
                if (manageChangesPrivileges == null || manageChangesPrivilegesPath == null) {
                    log.warn("Missing properties '{}' and/or '{}' at '{}' : Return empty SecurityModel.",
                            "manage.changes.privileges", "manage.changes.privileges.path", jcrPathTemplateComposer);
                    return mapping;
                }
                final PrivilegePathMapping manageChangesMappging = new PrivilegePathMapping(manageChangesPrivileges, manageChangesPrivilegesPath);
                mapping.put(CHANNEL_MANAGER_ADMIN_ROLE, manageChangesMappging);
                return mapping;
            } catch (RepositoryException e) {
                throw new IllegalStateException("Failed to build security model", e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
    }


    private void invalidate() {
        mappingModel = null;
    }

    private static class PrivilegePathMapping {

        private final String privilege;
        private final String privilegePath;

        public PrivilegePathMapping(final String privilege, final String privilegePath) {

            this.privilege = privilege;
            this.privilegePath = privilegePath;
        }
    }

    public static class SecurityModelEventListener extends GenericEventListener {

        private SecurityModelImpl securityModel;

        public void setSecurityModel(final SecurityModelImpl securityModel) {
            this.securityModel = securityModel;
        }

        @Override
        public void onEvent(final EventIterator events) {
            securityModel.invalidate();
        }
    }
}
