/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.security;


import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityModel {

    private static final Logger log = LoggerFactory.getLogger(SecurityModel.class);

    // at this moment the only supported functional roles
    public static final String CHANNEL_MANAGER_ADMIN_ROLE = "ChannelManagerAdmin";
    public static final String CHANNEL_WEBMASTER_ROLE = "ChannelWebmaster";


    private Repository repository;
    private Credentials credentials;
    private String jcrPathTemplateComposer;

    // mapping for [ role --> jcr privilege mapping ]
    private volatile Map<String, String> roleToPrivilegeMapping;

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void setJcrPathTemplateComposer(final String jcrPathTemplateComposer) {
        this.jcrPathTemplateComposer = jcrPathTemplateComposer;
    }

    public Principal getUserPrincipal(final Session session) {
        return session::getUserID;
    }


    /**
     * Below might be quite an odd implementation but this is because of legacy reasons how before it was found out
     * whether a user was an admin or webmaster. Hence this kind of awkward looking implementation
     */
    public boolean isUserInRole(final Session session, final String functionalRole) {

        final HstRequestContext requestContext = RequestContextProvider.get();
        final InternalHstModel model = (InternalHstModel)requestContext.getAttribute(PageComposerContextService.LIVE_EDITING_HST_MODEL_ATTR);
        if (model == null) {
            throw new IllegalStateException("Expected an HstModel to be present on request context");
        }

        if (CHANNEL_MANAGER_ADMIN_ROLE.equals(functionalRole)) {
            final Map<String, String> mapping = getRoleToJcrPrivilegeMapping();
            final String manageChangesRequiredPrivilege = mapping.get(functionalRole);
            if (manageChangesRequiredPrivilege == null) {
                log.info("No manage.changes.privileges for role '{}'.", functionalRole);
                return false;
            }
            try {

                return session.hasPermission(model.getConfigurationRootPath(), manageChangesRequiredPrivilege);
            } catch (RepositoryException e) {
                throw new IllegalStateException("Exception while checking permissions.", e);
            }
        } else if (CHANNEL_WEBMASTER_ROLE.equals(functionalRole)) {
            try {
                return session.hasPermission(model.getConfigurationRootPath() + "/accesstest", Session.ACTION_SET_PROPERTY);
            } catch (RepositoryException e) {
                log.warn("Could not determine authorization", e);
                throw new IllegalStateException("Exception while checking permissions.", e);
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported Functional role '%s'.", functionalRole));
    }

    private Map<String, String> getRoleToJcrPrivilegeMapping() {
        Map<String, String> mapping = roleToPrivilegeMapping;
        if (mapping != null) {
            return mapping;
        }
        synchronized (this) {
            mapping = roleToPrivilegeMapping;
            if (mapping != null) {
                return mapping;
            }

            mapping = new HashMap<>();
            roleToPrivilegeMapping = mapping;
            Session session = null;
            try {
                session = repository.login(credentials);

                final Node templateComposerNode = JcrUtils.getNodeIfExists(jcrPathTemplateComposer, session);
                if (templateComposerNode == null) {
                    log.warn("Missing jcr node at '{}' to read required configured admin privileges from: Return default hippo:admin " +
                                    "required privileges.",
                            jcrPathTemplateComposer);
                    mapping.put(CHANNEL_MANAGER_ADMIN_ROLE, "hippo:admin");
                    return mapping;
                }
                final String manageChangesPrivileges = JcrUtils.getStringProperty(templateComposerNode, "manage.changes.privileges", null);
                if (manageChangesPrivileges == null) {
                    log.warn("Missing properties '{}' and/or '{}' at '{}' : Return empty SecurityModel.",
                            "manage.changes.privileges", "manage.changes.privileges.path", jcrPathTemplateComposer);
                    return mapping;
                }
                mapping.put(CHANNEL_MANAGER_ADMIN_ROLE, manageChangesPrivileges);
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

}
