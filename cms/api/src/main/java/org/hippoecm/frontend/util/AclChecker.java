/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import java.security.AccessControlException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.PluginApplication;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for managing access control rights to different frontend applications
 * namely CMS and Console  
 */
public class AclChecker {

    private static final Logger log = LoggerFactory.getLogger(AclChecker.class);
    private static final String PRIVILEGES_CONFIGURATION_PARAM_POSTFIX = ".privileges";
    private static final String PRIVILEGES_PATH_CONFIGURATION_PARAM_POSTFIX = ".privileges.path";

    /**
     * Check whether a certain user which obtained a JCR session has access to a certain frontend application or not
     *
     * @param config - A plugin configuration from which some configuration parameters will be retrieved
     * @param session - A JCR session to check access with
     * @param prefix - The prefix to use to lookup configuration properties of the access control
     * @param defaultPrivilege - Default value for the privilege in case there is no one defined
     * @param defaultPath - Default value for privilege path in case there is no one defined
     * @throws AccessControlException If access is not allowed using {@code privilege}, either the configured or the
     * provided default value, to {@code path}, either the configured or the default value
     * @throws RepositoryException
     */
    public static void checkAccess(final IPluginConfig config, final Session session, final String prefix,
                                   final String defaultPrivilege, final String defaultPath) throws AccessControlException, RepositoryException {

        // Validate parameters
        if (config == null) {
            throw new IllegalArgumentException("A null configuration parameter has been passed");
        }

        if (session == null) {
            throw new IllegalArgumentException("A null session parameter has been passed");
        }

        final String aclPropertiesPrefix = (StringUtils.isBlank(prefix)) ? "" : prefix;
        String aclPrivilege = config.getString(aclPropertiesPrefix + PRIVILEGES_CONFIGURATION_PARAM_POSTFIX);
        aclPrivilege = (StringUtils.isBlank(aclPrivilege)) ? defaultPrivilege : aclPrivilege;
        String aclPrivilegePath = config.getString(aclPropertiesPrefix + PRIVILEGES_PATH_CONFIGURATION_PARAM_POSTFIX);
        aclPrivilegePath = (StringUtils.isBlank(aclPrivilegePath)) ? defaultPath : aclPrivilegePath;

        if (StringUtils.isBlank(aclPropertiesPrefix) || StringUtils.isBlank(aclPrivilege) || StringUtils.isBlank(aclPrivilegePath)) {
            log.info("No privileges check configured for application/component: '{}'", aclPropertiesPrefix);
            log.debug("No privileges check configured for application/component: '{}', privileges: '{}', and privileges path: '{}'",
                    new Object[] {aclPropertiesPrefix, aclPrivilege, aclPrivilegePath});

        } else {
            log.debug("Applying check for application/component '{}' with privileges: '{}' , and privileges path: '{}'",
                    new Object[] {aclPropertiesPrefix, aclPrivilege, aclPrivilegePath});

            session.checkPermission(aclPrivilegePath, aclPrivilege);
        }
    }

    public static void checkAccess(final IPluginConfig config, final Session session) throws AccessControlException,
            RepositoryException {

        checkAccess(config, session, PluginApplication.get().getPluginApplicationName());
    }


    public static void checkAccess(final IPluginConfig config, final Session session, final String prefix) throws AccessControlException,
            RepositoryException {

        checkAccess(config, session, prefix, null, null);
    }

}
