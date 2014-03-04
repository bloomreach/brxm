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

package org.onehippo.cms7.essentials.components.cms.modules;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.components.cms.blog.BlogUpdater;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.modules.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The EventBusListenerModule is a daemon module started by the CMS (based on the corresponding hippo:modules
 * repository configuration. It registers a HippoEventBus listener, which then dispatches events. Currently,
 * we're only interested in the "save" event for blogposts. The blog-post specific code can be found in the
 * BlogUpdater class.
 */
public class EventBusListenerModule implements DaemonModule {

    public static final Logger log = LoggerFactory.getLogger(EventBusListenerModule.class);

    // Regrettably, the CMS doesn't expose below string in an API...
    private static final String METHOD_NAME_SAVE = "commitEditableInstance";

    private Session session;
    private EventBusListener listener;

    /**
     * Initialization of daemon module, register listener.
     * @param session module's JCR session
     */
    @Override
    public void initialize(Session session) {
        this.session = session;
        listener = new EventBusListener();
        HippoServiceRegistry.registerService(listener, HippoEventBus.class);
    }

    /**
     * Shutdown fo daemon module, deregister listener.
     */
    @Override
    public void shutdown() {
        HippoServiceRegistry.unregisterService(listener, HippoEventBus.class);
    }

    /**
     * Helper function to derive a certain document variant, given a hippo:mirror node.
     * Optimally, the CMS or repository would provide this functionality.
     * @param mirror repository node of type hippo:mirror
     * @param state  desired state of the variant
     * @return       JCR node representing that variant, or null.
     * @throws javax.jcr.RepositoryException
     */
    public static Node getReferencedVariant(final Node mirror, final String state) throws RepositoryException {
        final Session session = mirror.getSession();
        final String rootUuid = session.getRootNode().getIdentifier();
        final String uuid = mirror.getProperty("hippo:docbase").getString();
        Node variant = null;
        if (!rootUuid.equals(uuid)) {
            final Node authorHandle = session.getNodeByIdentifier(uuid);
            variant = getVariant(authorHandle, state);
        }
        return variant;
    }

    /**
     *
     * @param handle JCR node representing a handle
     * @param state  desired state of the variant
     * @return       JCR node representing that variant, or null.
     * @throws javax.jcr.RepositoryException
     */
    public static Node getVariant(final Node handle, final String state) throws RepositoryException {
        final NodeIterator variants = handle.getNodes(handle.getName());
        while (variants.hasNext()) {
            final Node variant = variants.nextNode();
            if (variant.hasProperty("hippostd:state")
                && variant.getProperty("hippostd:state").getString().equals(state))
            {
                return variant;
            }
        }
        return null;
    }

    /**
     * The actual listener being called by the Hippo Event Bus.
     */
    private class EventBusListener {

        /**
         * Dispatch the event per workflow action.
         * @param event the event.
         */
        @Subscribe
        public void handleEvent(HippoEvent<?> event) {
            if (HippoEventConstants.CATEGORY_WORKFLOW.equals(event.category())) {
                HippoWorkflowEvent<?> wfEvent = new HippoWorkflowEvent(event);

                if (METHOD_NAME_SAVE.equals(wfEvent.methodName())) {
                    dispatchSaveEvent(wfEvent);
                }
            }
        }

        /**
         * Dispatch a "save" event.
         * Derive the unpublished variant of the document the save event pertains to, and check who wants it.
         * @param event the event.
         */
        private void dispatchSaveEvent(HippoWorkflowEvent<?> event) {
            final String handleUuid = event.handleUuid();
            try {
                final Node handle = session.getNodeByIdentifier(handleUuid);
                final Node variant = getVariant(handle, "unpublished");
                boolean doSave = false;
                if (variant != null) {
                    if (BlogUpdater.wants(variant)) {
                        doSave = BlogUpdater.handleSaved(variant);
                    }
                }
                if (doSave) {
                    session.save();
                }
            } catch (RepositoryException ex) {
                log.debug("Failed to process node for handle UUID '" + handleUuid + "'.", ex);
            }
        }
    }
}
