/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingUtils {

    private static final Logger log = LoggerFactory.getLogger(EditingUtils.class);

    private EditingUtils() {

    }

    public static Optional<Node> createDraft(final EditableWorkflow workflow, final Session session) {
        try {
            final Document document = workflow.obtainEditableInstance();
            return Optional.of(document.getNode(session));
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to obtain draft for user '{}'.", session.getUserID(), e);
        }
        return Optional.empty();
    }

    public static Optional<Node> copyToPreviewAndKeepEditing(final EditableWorkflow workflow, final Session session) {
        try {
            workflow.commitEditableInstance();
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to commit changes for user '{}'.", session.getUserID(), e);
            return Optional.empty();
        }

        return createDraft(workflow, session);
    }

}
