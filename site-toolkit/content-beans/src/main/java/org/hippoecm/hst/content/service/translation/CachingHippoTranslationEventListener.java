/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.service.translation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachingHippoTranslationEventListener extends GenericEventListener
        implements EventListenersContainerListener {

    private static final Logger log = LoggerFactory.getLogger(CachingHippoTranslationEventListener.class);

    private final Repository repository;

    private final Credentials credentials;

    private HippoTranslationContentRegistry hippoTranslationContentRegistry;

    public CachingHippoTranslationEventListener(final Repository repository, final Credentials credentials) {
        this.repository = repository;
        this.credentials = credentials;
    }

    @Override
    public void onEvent(EventIterator events) {
        hippoTranslationContentRegistry = HstServices.getComponentManager().getComponent(HippoTranslationContentRegistry.class.getName());

        if (hippoTranslationContentRegistry == null) {
            return;
        }

        Event event;

        while (events.hasNext()) {
            try {
                event = events.nextEvent();

                if (eventIgnorable(event)) {
                    continue;
                }

                switch (event.getType()) {
                case Event.NODE_ADDED:
                    handleNodeAdded(event);
                    break;
                case Event.NODE_REMOVED:
                    handleNodeRemoved(event);
                    break;
                }
            } catch (RepositoryException e) {
                log.error("Error processing event");
            }
        }
    }

    @Override
    public void onEventListenersContainerStarted() {
        // do nothing for now
    }

    @Override
    public void onEventListenersContainerRefreshed() {
        // do nothing
    }

    @Override
    public void onEventListenersContainerStopped() {
    }

    private void handleNodeAdded(final Event event) throws RepositoryException {
        Session session = null;

        try {
            session = repository.login(credentials);
            Node node = session.getNodeByIdentifier(event.getIdentifier());

            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                String translationId = findTranslationIdFromHandleNode(node);

                if (translationId != null) {
                    List<Node> translationNodes = HippoTranslatedContentUtils.findTranslationNodes(session,
                            translationId);
                    Set<String> documentHandleIds = new HashSet<>();
                    Node handleNode;

                    for (Node translationNode : translationNodes) {
                        handleNode = translationNode.getParent();

                        if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                            documentHandleIds.add(handleNode.getIdentifier());
                        }
                    }

                    hippoTranslationContentRegistry.putDocumentHandleIdsForTranslationId(translationId,
                            documentHandleIds);
                }
            }
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    private void handleNodeRemoved(final Event event) throws RepositoryException {
        Session session = null;

        try {
            session = repository.login(credentials);
            String nodeId = event.getIdentifier();
            hippoTranslationContentRegistry.removeDocumentHandleId(nodeId);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    private String findTranslationIdFromHandleNode(final Node handle) throws RepositoryException {
        String translationId = null;

        if (handle.hasNode(handle.getName())) {
            Node variant;

            for (NodeIterator nodeIt = handle.getNodes(handle.getName()); nodeIt.hasNext();) {
                variant = nodeIt.nextNode();

                if (variant.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)
                        && variant.hasProperty(HippoTranslationNodeType.ID)) {
                    translationId = StringUtils
                            .trimToNull(variant.getProperty(HippoTranslationNodeType.ID).getString());

                    if (translationId != null) {
                        break;
                    }
                }
            }
        }

        return translationId;
    }
}
