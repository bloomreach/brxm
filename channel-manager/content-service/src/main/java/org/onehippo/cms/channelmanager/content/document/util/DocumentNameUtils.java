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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecService;
import org.hippoecm.repository.api.StringCodecService.Encoding;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentNameUtils {

    private static final String WORKFLOW_CATEGORY_CORE = "core";
    private static final Logger log = LoggerFactory.getLogger(DocumentNameUtils.class);

    private DocumentNameUtils() {
    }

    public static String encodeUrlName(final String urlName, final String locale) {
        return encode(urlName, Encoding.NODE_NAME, locale);
    }

    public static String encodeDisplayName(final String displayName, final String locale) {
        return encode(displayName, Encoding.DISPLAY_NAME, locale);
    }

    private static String encode(final String name, final Encoding encoding, final String locale) {
        if (name == null) {
            return null;
        }
        final StringCodecService service = HippoServiceRegistry.getService(StringCodecService.class);
        final StringCodec codec = service.getStringCodec(encoding, locale);
        return codec.encode(name);
    }

    public static void setNames(final Node node, final String urlName, final String displayName, final String locale) {
        final String oldUrlName = JcrUtils.getNodeNameQuietly(node);
        final String oldDisplayName = DocumentUtils.getDisplayName(node).orElse(StringUtils.EMPTY);

        final String newUrlName = encodeUrlName(urlName, locale);
        final String newDisplayName = encodeDisplayName(displayName, locale);

        final boolean sameUrlName = StringUtils.equals(oldUrlName, newUrlName);
        final boolean sameDisplayName = StringUtils.equals(oldDisplayName, newDisplayName);

        if (sameUrlName && sameDisplayName) {
            // nothing to update
            return;
        }

        WorkflowUtils.getWorkflow(node, WORKFLOW_CATEGORY_CORE, DefaultWorkflow.class)
                .ifPresent((workflow) -> {
                    if (!sameUrlName) {
                        rename(node, workflow, newUrlName);
                    }
                    if (!sameDisplayName) {
                        setDisplayName(node, workflow, newDisplayName);
                    }
                });
    }

    private static void rename(final Node node, final DefaultWorkflow workflow, final String name) {
        try {
            workflow.rename(name);
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.warn("Failed to rename node '{}' to '{}'", JcrUtils.getNodePathQuietly(node), name, e);
        }
    }

    public static void setDisplayName(final Node node, final String displayName, final String locale) {
        WorkflowUtils.getWorkflow(node, WORKFLOW_CATEGORY_CORE, DefaultWorkflow.class)
                .ifPresent((workflow) -> {
                    final String encodedDisplayName = encodeDisplayName(displayName, locale);
                    setDisplayName(node, workflow, encodedDisplayName);
                });
    }

    private static void setDisplayName(final Node node, final DefaultWorkflow workflow, final String displayName) {
        try {
            workflow.setDisplayName(displayName);
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.warn("Failed to set display name of node '{}' to '{}'",
                    JcrUtils.getNodePathQuietly(node), displayName, e);
        }
    }
}
