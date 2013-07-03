/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionUtils {

    private static final Logger log = LoggerFactory.getLogger(JcrSessionUtils.class);

    private JcrSessionUtils() {
    }

    /**
     * @param prune Wheter only to return the first matching modified node in
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
     * @param prune Wheter only to return the first matching modified node in
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
}
