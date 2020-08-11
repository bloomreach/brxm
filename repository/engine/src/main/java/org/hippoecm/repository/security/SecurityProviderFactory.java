/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.security.SecurityConstants;

public class SecurityProviderFactory {

    private final String securityPath = SecurityConstants.CONFIG_SECURITY_PATH.substring(1);
    private final String usersPath = SecurityConstants.CONFIG_USERS_PATH.substring(1);
    private final String groupsPath = SecurityConstants.CONFIG_GROUPS_PATH.substring(1);
    private final String rolesPath = SecurityConstants.CONFIG_ROLES_PATH.substring(1);
    private final String domainsPath = SecurityConstants.CONFIG_DOMAINS_PATH.substring(1);
    private boolean isMaintenance = false;

    public SecurityProviderFactory(boolean isMaintenance) {
        this.isMaintenance = isMaintenance;
    }

    /**
     * Create and initialize a new security provider base on the providerNode.
     * The providerNode has a property hipposys:classname which defines the class
     * to be instantiated.
     * @param providerNode the provider jcr node with the configuration
     * @return SecurityProvider
     * @throws ValueFormatException
     * @throws PathNotFoundException
     * @throws RepositoryException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public SecurityProvider create(Session session, String providerId) throws ValueFormatException, PathNotFoundException, RepositoryException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Node providerNode = session.getRootNode().getNode(securityPath + "/" + providerId);
        Class<?> clazz = Class.forName(providerNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString());
        SecurityProvider sp = (SecurityProvider) clazz.newInstance();

        // create new session for each provider
        Session providerSession = session.impersonate(new SimpleCredentials("system", new char[] {}));
        SecurityProviderContext context = new SecurityProviderContext(providerSession, providerId, securityPath, usersPath, groupsPath, rolesPath, domainsPath, isMaintenance);
        sp.init(context);
        return sp;
    }

}
