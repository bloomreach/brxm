/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdPubWfNodeType.NT_HIPPOSTDPUBWF_REQUEST;

public class DocumentStateUtils {

    private static final Logger log = LoggerFactory.getLogger(DocumentStateUtils.class);

    private DocumentStateUtils() {
    }

    public static List<WorkflowRequest> getWorkflowRequests(final Node handle) {
        if (!isValidHandle(handle)) {
            return Collections.emptyList();
        }

        final List<WorkflowRequest> requests = new LinkedList<>();
        try {
            final NodeIterator requestNodes = handle.getNodes(HippoNodeType.HIPPO_REQUEST);
            while (requestNodes.hasNext()) {
                final Node requestNode = requestNodes.nextNode();
                if (requestNode.isNodeType(NT_HIPPOSTDPUBWF_REQUEST)) {
                    requests.add(new WorkflowRequest(requestNode));
                }
            }
        } catch (RepositoryException e) {
            log.warn("Error retrieving workflow request(s) for document '{}'", JcrUtils.getNodePathQuietly(handle), e);
        }

        return requests;
    }

    public static ScheduledRequest getScheduledRequest(final Node handle) {
        if (!isValidHandle(handle)) {
            return null;
        }

        try {
            final NodeIterator requestNodes = handle.getNodes(HippoNodeType.HIPPO_REQUEST);
            while (requestNodes.hasNext()) {
                final Node requestNode = requestNodes.nextNode();
                if (requestNode.isNodeType("hipposched:workflowjob")) {
                    return new ScheduledRequest(requestNode);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Could not retrieve workflow request of document '{}'", JcrUtils.getNodePathQuietly(handle), e);
        }
        return null;
    }

    private static boolean isValidHandle(final Node handle) {
        return DocumentUtils.getVariantNodeType(handle)
                .filter(type -> !type.equals(HippoNodeType.NT_DELETED))
                .isPresent();
    }
}
