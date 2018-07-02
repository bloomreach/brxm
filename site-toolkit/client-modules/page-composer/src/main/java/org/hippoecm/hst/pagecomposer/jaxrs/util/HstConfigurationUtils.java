/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.platform.api.model.PlatformHstModel;
import org.hippoecm.hst.platform.model.HstModelImpl;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static org.hippoecm.hst.configuration.HstNodeTypes.CONFIGURATION_PROPERTY_LOCKED;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.PREVIEW_EDITING_HST_MODEL_ATTR;
import static org.hippoecm.hst.util.JcrSessionUtils.getPendingChangePaths;


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
        // for logging use pruned paths because the non-pruned path changes can be very large
        String[] prunedPathChanges = filterOutUpstream(getPendingChangePaths(session, true));

        // never prune for getting changes since needed for hstNode model reloading
        String[] pathsToBeChanged = filterOutUpstream(getPendingChangePaths(session, false));

        setLastModifiedTimeStamps(session, pathsToBeChanged);

        session.save();
        EventPathsInvalidator invalidator = getPreviewHstModel().getEventPathsInvalidator();
        // after the save the paths need to be send, not before!
        if (invalidator != null && pathsToBeChanged != null) {
            invalidator.eventPaths(pathsToBeChanged);
        }
        //only log when the save is successful
        logEvent("write-changes",session.getUserID(),StringUtils.join(prunedPathChanges, ","));
    }

    // since the changes might be in the hashes in hst:upstream or below, and those changes are not interesting for
    // model reloading *nor* for the model itself, we won't send them as event log either
    static String[] filterOutUpstream(final String[] pendingChangePaths) {
        String containsUpstream = "/" + HstNodeTypes.NODENAME_HST_UPSTREAM + "/";
        String endsWithUpstream = "/" + HstNodeTypes.NODENAME_HST_UPSTREAM;

        String[] filtered = asList(pendingChangePaths).stream()
                .filter((path) -> (!path.contains(containsUpstream) && !path.endsWith(endsWithUpstream)))
                .toArray(String[]::new);
        return filtered;
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

    /**
     * Creates preview configuration as well as mandatory workspace nodes (hst:pages and hst:sitemap) in the live configuration
     * if they are not yet present. This method does <strong>not</strong> persist the changes
     */
    public static void createPreviewConfiguration(final String liveConfigurationPath, final Session session) throws RepositoryException, ClientException {
        HstConfigurationUtils.createMandatoryWorkspaceNodesIfMissing(liveConfigurationPath, session);
        final String liveConfigName = StringUtils.substringAfterLast(liveConfigurationPath, "/");
        final Node previewConfigNode = session.getNode(liveConfigurationPath).getParent().addNode(liveConfigName + "-preview", NODETYPE_HST_CONFIGURATION);
        previewConfigNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../" + liveConfigName});
        final  Node liveConfigNode = session.getNode(liveConfigurationPath);
        if (liveConfigNode.hasProperty(CONFIGURATION_PROPERTY_LOCKED)) {
            // make sure the preview has same value for locked
            previewConfigNode.setProperty(CONFIGURATION_PROPERTY_LOCKED, liveConfigNode.getProperty(CONFIGURATION_PROPERTY_LOCKED).getBoolean());
        }
        JcrUtils.copy(session, liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE, previewConfigNode.getPath() + "/" + NODENAME_HST_WORKSPACE);
    }

    /**
     * Creates hst:pages and hst:sitemap in workspace if they are not yet present.
     * This method does <strong>not</strong> persist the changes
     */
    public static void createMandatoryWorkspaceNodesIfMissing(final String configPath, final Session session) throws RepositoryException {
        if (!session.nodeExists(configPath)) {
            String msg = String.format("Expected configuration node at '%s'", configPath);
            throw new ClientException(msg, ClientError.ITEM_NOT_FOUND);
        }
        Node configNode = session.getNode(configPath);
        if (configNode.hasNode(HstNodeTypes.NODENAME_HST_WORKSPACE)) {
            Node workspace = configNode.getNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
            if (!workspace.hasNode(HstNodeTypes.NODENAME_HST_PAGES)) {
                workspace.addNode(HstNodeTypes.NODENAME_HST_PAGES);
            }
            if (!workspace.hasNode(HstNodeTypes.NODENAME_HST_SITEMAP)) {
                workspace.addNode(HstNodeTypes.NODENAME_HST_SITEMAP);
            }
        } else {
            Node workspace = configNode.addNode(HstNodeTypes.NODENAME_HST_WORKSPACE);
            workspace.addNode(HstNodeTypes.NODENAME_HST_PAGES);
            workspace.addNode(HstNodeTypes.NODENAME_HST_SITEMAP);
        }
    }


    public static VirtualHosts getEditingPreviewVirtualHosts() {
        return getPreviewHstModel().getVirtualHosts();
    }

    public static PlatformHstModel getPreviewHstModel() {
        return (PlatformHstModel)getRequestContext().getAttribute(PREVIEW_EDITING_HST_MODEL_ATTR);
    }
    public static HstRequestContext getRequestContext() {
        return RequestContextProvider.get();
    }

}
