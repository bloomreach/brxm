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
package org.hippoecm.hst.pagecomposer.jaxrs.util;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HstConfigurationUtils {

    private static final Logger log = LoggerFactory.getLogger(HstConfigurationUtils.class);

    private HstConfigurationUtils() {
    }

    /**
     * Persists pending changes. logs events to the HippoEventBus and if <code>invalidator</code> is not <code>null</code>
     * also send event paths
     * @param session
     * @throws RepositoryException
     */
    public synchronized static void persistChanges(final Session session) throws RepositoryException {
        if (!session.hasPendingChanges()) {
            return;
        }
        // never prune for getting changes since needed for hstNode model reloading
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, false);

        setLastModifiedTimeStamps(session, pathsToBeChanged);

        session.save();
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        // after the save the paths need to be send, not before!
        if (invalidator != null && pathsToBeChanged != null) {
            invalidator.eventPaths(pathsToBeChanged);
        }
        //only log when the save is successful
        logEvent("write-changes",session.getUserID(),StringUtils.join(pathsToBeChanged, ","));
    }


    // set a lastmodified timestamp on all changed nodes of type
    // 1) hst:containercomponent OR
    // 2) hst:editable OR
    // OR on the parent of all changed hst:containeritemcomponent if that parent is a hst:containercomponent
    private static void setLastModifiedTimeStamps(final Session session, final String[] pathsToBeChanged) throws RepositoryException {

        final Calendar cal = Calendar.getInstance();
        for (String nodePath : pathsToBeChanged) {
            Node changedNode = null;
            if (session.nodeExists(nodePath)) {
               changedNode = session.getNode(nodePath);
            } else {
                // check if parent exists and possibly mark that one. Otherwise continue
                String parentPath = StringUtils.substringBeforeLast(nodePath, "/");
                if (StringUtils.isEmpty(parentPath)) {
                    continue;
                }
                if (session.nodeExists(parentPath)) {
                    changedNode = session.getNode(parentPath);
                }
            }
            if (changedNode == null) {
                continue;
            }

            if (changedNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                // parent should get the versionStamp
                changedNode = changedNode.getParent();
            }

            if ( changedNode.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)
                    || changedNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT)) {
                changedNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, cal);
            }
        }
    }


    public static void logEvent(String action, String user, String message) {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoEvent event = new HippoEvent("channel-manager");
            event.category("channel-manager").user(user).action(action);
            event.message(message);
            eventBus.post(event);
        }
    }

    public static HippoSession getNonProxiedSession(Session session) throws RepositoryException {
        HippoSession hippoSession;
        if (!(session instanceof HippoSession)) {
            // a jcr session from request context cannot be directly cast to a HippoSession...hence this workaround:
            Session nonProxiedSession = session.getRootNode().getSession();
            if (!(nonProxiedSession instanceof HippoSession)) {
                throw new IllegalStateException("Session not instance of HippoSession.");
            }
            hippoSession = (HippoSession) nonProxiedSession;
        } else {
            hippoSession = (HippoSession) session;
        }
        return hippoSession;
    }


}
