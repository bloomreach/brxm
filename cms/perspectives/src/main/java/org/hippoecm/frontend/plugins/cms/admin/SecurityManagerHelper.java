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
package org.hippoecm.frontend.plugins.cms.admin;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.session.UserSession;

import com.bloomreach.xm.repository.security.ChangePasswordManager;
import com.bloomreach.xm.repository.security.DomainsManager;
import com.bloomreach.xm.repository.security.RepositorySecurityManager;
import com.bloomreach.xm.repository.security.RolesManager;
import com.bloomreach.xm.repository.security.RolesProvider;
import com.bloomreach.xm.repository.security.UserRolesManager;
import com.bloomreach.xm.repository.security.UserRolesProvider;

/**
 * Convenient static helper methods for accessing the {@link RepositorySecurityManager} and its
 * provided managers via the current CMS session.
 */
public final class SecurityManagerHelper {

    private SecurityManagerHelper() {}

    public static RepositorySecurityManager getSecurityManager() {
        return UserSession.get().getJcrSession().getWorkspace().getSecurityManager();
    }

    public static RolesProvider getRolesProvider() {
        return getSecurityManager().getRolesProvider();
    }

    public static UserRolesProvider getUserRolesProvider() {
        return getSecurityManager().getUserRolesProvider();
    }

    public static ChangePasswordManager getChangePasswordManager() throws AccessDeniedException, RepositoryException {
        return getSecurityManager().getChangePasswordManager();
    }

    public static RolesManager getRolesManager() throws AccessDeniedException, RepositoryException {
        return getSecurityManager().getRolesManager();
    }

    public static UserRolesManager getUserRolesManager() throws AccessDeniedException, RepositoryException {
        return getSecurityManager().getUserRolesManager();
    }

    public static DomainsManager getDomainsManager() throws AccessDeniedException, RepositoryException {
        return getSecurityManager().getDomainsManager();
    }
}
