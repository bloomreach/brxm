/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.cms.handlers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.components.cms.blog.BlogUpdater;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class AuthorFieldHandler implements WorkflowEventHandler {

    private static Logger log = LoggerFactory.getLogger(AuthorFieldHandler.class);

    // Regrettably, the CMS doesn't expose below string in an API...
    private static final String METHOD_NAME_SAVE = "commitEditableInstance";


    private final String projectNamespacePath;


    public AuthorFieldHandler(final String projectNamespacePath) {
        this.projectNamespacePath = projectNamespacePath;
    }

    /**
     * Dispatch the event per workflow action.
     *
     * @param event the event.
     */
    @Override
    @Subscribe
    public void handle(final HippoEvent<?> event, final Session session) {
        if (HippoEventConstants.CATEGORY_WORKFLOW.equals(event.category())) {
            HippoWorkflowEvent<?> wfEvent = new HippoWorkflowEvent(event);

            if (METHOD_NAME_SAVE.equals(wfEvent.methodName())) {
                dispatchSaveEvent(wfEvent, session);
            }
        }
    }

    /**
     * Dispatch a "save" event.
     * Derive the unpublished variant of the document the save event pertains to, and check who wants it.
     *
     * @param event   the event.
     * @param session
     */
    @SuppressWarnings("HippoHstCallNodeRefreshInspection")
    private void dispatchSaveEvent(HippoWorkflowEvent<?> event, final Session session) {
        final String handleUuid = event.subjectId();
        try {
            final Node handle = session.getNodeByIdentifier(handleUuid);
            final Node variant = HandlerUtils.getVariant(handle, "unpublished");
            boolean doSave = false;
            if (variant != null) {
                if (session.propertyExists(projectNamespacePath)) {
                    final String projectNamespace = session.getProperty(projectNamespacePath).getString();
                    if (Strings.isNullOrEmpty(projectNamespace)) {
                        log.warn("projectNamespace property not set @{}", projectNamespacePath);
                        return;
                    }

                    if (BlogUpdater.wants(variant, projectNamespace + ":blogpost")) {
                        doSave = BlogUpdater.handleSaved(variant, projectNamespace);
                    }
                } else {
                    log.warn("projectNamespace property not set");
                }
            }
            if (doSave) {
                session.save();
            }
        } catch (RepositoryException ex) {
            log.debug("Failed to process node for handle UUID '" + handleUuid + "'.", ex);
            try {
                session.refresh(false);
            } catch (RepositoryException e) {
                log.error("Error refreshing session", e);
            }
        }
    }
}
