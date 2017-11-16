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

import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecService;
import org.hippoecm.repository.api.StringCodecService.Encoding;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentNameUtils {

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

    public static String getUrlName(final Node handle) throws InternalServerErrorException {
        try {
            return handle.getName();
        } catch (RepositoryException e) {
            log.warn("Failed to read name of node '{}'", JcrUtils.getNodePathQuietly(handle));
            throw new InternalServerErrorException();
        }
    }

    public static void setUrlName(final Node handle, final String urlName) throws ForbiddenException, InternalServerErrorException, MethodNotAllowed {
        final DocumentWorkflow documentWorkflow = ContentWorkflowUtils.getDocumentWorkflow(handle);

        if (EditingUtils.canRenameDocument(documentWorkflow)) {
            renameDocument(handle, urlName, documentWorkflow);
        } else if (EditingUtils.hasPreview(documentWorkflow)) {
            log.warn("Cannot change the URL name of document '{}': it already has a preview variant",
                    JcrUtils.getNodePathQuietly(handle));
            throw new ForbiddenException();
        } else {
            renameDraft(handle, urlName);
        }
    }

    private static void renameDocument(final Node handle, final String urlName, final DocumentWorkflow documentWorkflow) throws InternalServerErrorException {
        try {
            documentWorkflow.rename(urlName);
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.warn("Failed to rename document '{}' to '{}'", JcrUtils.getNodePathQuietly(handle), urlName, e);
            throw new InternalServerErrorException();
        }
    }

    private static void renameDraft(final Node handle, final String urlName) throws InternalServerErrorException, MethodNotAllowed {
        final DefaultWorkflow workflow = ContentWorkflowUtils.getDefaultWorkflow(handle);
        try {
            workflow.rename(urlName);
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.warn("Failed to rename draft '{}' to '{}'", JcrUtils.getNodePathQuietly(handle), urlName, e);
            throw new InternalServerErrorException();
        }
    }

    public static String getDisplayName(final Node handle) throws InternalServerErrorException {
        return DocumentUtils.getDisplayName(handle).orElseThrow(InternalServerErrorException::new);
    }

    public static void setDisplayName(final Node handle, final String displayName) throws MethodNotAllowed, InternalServerErrorException {
        final DefaultWorkflow workflow = ContentWorkflowUtils.getDefaultWorkflow(handle);

        try {
            workflow.setDisplayName(displayName);
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.warn("Failed to set display name of node '{}' to '{}'",
                    JcrUtils.getNodePathQuietly(handle), displayName, e);
            throw new InternalServerErrorException();
        }
    }
}
