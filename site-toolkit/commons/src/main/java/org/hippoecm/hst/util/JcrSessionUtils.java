/*
 *  Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionUtils {

    private static final Logger log = LoggerFactory.getLogger(JcrSessionUtils.class);

    private JcrSessionUtils() {
    }

    /**
     * @param prune Whether only to return the first matching modified node in
     *              a subtree (true), or provide a depth search for all modified
     *              nodes (false)
     * @return String array of all events path
     */
    public static String[] getPendingChangePaths(Session session, boolean prune) throws RepositoryException {
        return getPendingChangePaths(session, session.getRootNode(), prune);
    }
    /**
     * @param node The starting node for which to look for changes, will not
     *             be included in result, may be null to indicate to search whole tree
     * @param prune Whether only to return the first matching modified node in
     *              a subtree (true), or provide a depth search for all modified
     *              nodes (false)
     * @return String array of all events path
     */
    public static String[] getPendingChangePaths(Session session, Node node, boolean prune) throws RepositoryException {
        HippoSession hippoSession = getHippoSession(session);
        final NodeIterator pendingNodes = hippoSession.pendingChanges(node, "nt:base", prune);
        List<String> changePaths = new ArrayList<String>();
        while (pendingNodes.hasNext()) {
            changePaths.add(pendingNodes.nextNode().getPath());
        }
        return changePaths.toArray(new String[changePaths.size()]);
    }

    public static HippoSession getHippoSession(final Session session) throws RepositoryException, IllegalArgumentException {
        if (!(session instanceof HippoSession)) {
            // a jcr session from request context cannot be directly cast to a HippoSession...hence this workaround:
            Session nonProxiedSession = session.getRootNode().getSession();
            if (!(nonProxiedSession instanceof HippoSession)) {
                log.error("Session not instance of HippoSession. Cannot get pending changes");
                throw new IllegalArgumentException("Session not instance of HippoSession. Cannot get pending changes");
            }
            return (HippoSession) nonProxiedSession;
        } else {
            return (HippoSession) session;
        }
    }

    /**
     *
     * @param session the Session to check the privilege for
     * @param absPath the absolute jcr path to test the privilege against
     * @param requiredPrivilege
     * @return {@code true} in case the {@code session} has privilege {@code requiredPrivilege} and false otherwise (also
     * in case some exception happens)
     */
    public static boolean isInRole(final Session session, final String absPath,
                     final String requiredPrivilege) {

        try {
            return Arrays.stream(session.getAccessControlManager()
                    .getPrivileges(absPath))
                    .anyMatch(privilege -> privilege.getName().equals(requiredPrivilege));
        } catch (RepositoryException e) {
            log.warn("Exception while checking privilege", e);
            return false;
        }
    }

    /**
     * <p>
     *     Utility method to return the <strong>CMS</strong> user {@link Session}. Note that accessing a {@link Session}
     *     in Channel Mgr Preview mode via for example {@link HstRequestContext#getContentBean()} and then via the
     *     {@link Node} from {@link HippoBean#getNode()} does return a <strong>different</strong> {@link Session} : It
     *     namely returns a {@link Session} delegate: A combination of the HST preview user and the cms user.
     * </p>
     * <p>
     *     Note this does not create a new {@link Session} : Code getting hold of the CMS {@link Session} should NEVER
     *     log this {@link Session} out since this is managed by the HST
     * </p>
     * @param requestContext
     * @return the cms {@link Session} in case the {@link HstRequestContext} is for a channel mgr preview request
     * @throws IllegalStateException in case the request is not for a channel mgr preview request or in case there is
     * no cms user session available on the HstRequestContext
     */
    public static HippoSession getCmsUser(final HstRequestContext requestContext) {
        if (!requestContext.isChannelManagerPreviewRequest()) {
            throw new IllegalStateException("Cms User is only available for Channel Manager preview requests");
        }
        final HippoSession cmsUser = (HippoSession) requestContext.getAttribute(ContainerConstants.CMS_USER_SESSION_ATTR_NAME);
        if (cmsUser == null) {
            throw new IllegalStateException("For Channel Manager preview requests there is expected to be a CMS user " +
                    "Session available");
        }
        return cmsUser;
    }
}
