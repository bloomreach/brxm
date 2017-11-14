/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.document.util;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecService;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisplayNameUtils {

    private static final String WORKFLOW_CATEGORY_CORE = "core";
    private static final Logger log = LoggerFactory.getLogger(DisplayNameUtils.class);

    private DisplayNameUtils() {
    }

    public static String encodeDisplayName(final String displayName, final String locale) {
        final StringCodecService service = HippoServiceRegistry.getService(StringCodecService.class);
        final StringCodec codec = service.getStringCodec(StringCodecService.Encoding.DISPLAY_NAME, locale);
        return codec.encode(displayName);
    }

    public static void setDisplayName(final Node node, final String displayName) {
        WorkflowUtils.getWorkflow(node, WORKFLOW_CATEGORY_CORE, DefaultWorkflow.class)
                .ifPresent((workflow) -> {
                    try {
                        workflow.setDisplayName(displayName);
                    } catch (RepositoryException | WorkflowException | RemoteException e) {
                        log.warn("Failed to set display name of node '{}' to '{}'",
                                JcrUtils.getNodePathQuietly(node), displayName, e);
                    }
                });
    }

    public static String getDisplayName(final Node node) throws RepositoryException {
        if (node instanceof HippoNode) {
            return ((HippoNode) node).getDisplayName();
        }
        return node.getName();
    }
}
