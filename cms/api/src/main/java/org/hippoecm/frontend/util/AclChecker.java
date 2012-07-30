/*
 *  Copyright 2012 Hippo.
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
     * @throws RepositoryException If any repository error happened
     * @throws AccessControlException If permission is denied
     */
    public static void checkAccess(IPluginConfig config, Session session) throws AccessControlException,
            RepositoryException {

        // Validate parameters
        if (config == null) {
            throw new IllegalArgumentException("A null configuration parameter has been passed");
        }

        if (session == null) {
            throw new IllegalArgumentException("A null session parameter has been passed");
        }

        // Do the magic here
        final String applicationName = WebApplicationHelper.getApplicationName("cms");
        final String privileges = config.getString(applicationName + PRIVILEGES_CONFIGURATION_PARAM_POSTFIX);
        final String privilegesPath = config.getString(applicationName + PRIVILEGES_PATH_CONFIGURATION_PARAM_POSTFIX);

        if (StringUtils.isBlank(privileges) || StringUtils.isBlank(privilegesPath)) {
            log.info("No privileges check configured for application: " + applicationName);
            log.debug("No privileges check configured for application: " + applicationName + ", privileges: " + privileges + ", and privileges path: "
                    + privilegesPath);
        } else {
            log.debug("Applying check for application " + applicationName + " with privileges: " + privileges + ", and privileges path: "
                    + privilegesPath);
            session.checkPermission(privilegesPath, privileges);
        }
    }

}
