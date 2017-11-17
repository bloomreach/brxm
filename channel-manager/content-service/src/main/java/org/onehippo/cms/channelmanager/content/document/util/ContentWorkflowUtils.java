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

import javax.jcr.Node;

import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.withDisplayName;

public class ContentWorkflowUtils {

    private static final String WORKFLOW_CATEGORY_CORE = "core";
    private static final String WORKFLOW_CATEGORY_DEFAULT = "default";
    private static final String WORKFLOW_CATEGORY_EDIT = "editing";
    private static final String WORKFLOW_CATEGORY_INTERNAL = "internal";

    private ContentWorkflowUtils() {
    }

    public static DefaultWorkflow getDefaultWorkflow(final Node handle) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_CORE, DefaultWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(Reason.NOT_A_DOCUMENT), handle)
                ));
    }

    public static DocumentWorkflow getDocumentWorkflow(final Node handle) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_DEFAULT, DocumentWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(Reason.NOT_A_DOCUMENT), handle)
                ));
    }

    public static FolderWorkflow getFolderWorkflow(final Node folder) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(folder, WORKFLOW_CATEGORY_INTERNAL, FolderWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(Reason.NOT_A_FOLDER), folder)
                ));
    }

    public static EditableWorkflow getEditableWorkflow(final Node handle) throws MethodNotAllowed {
        return WorkflowUtils.getWorkflow(handle, WORKFLOW_CATEGORY_EDIT, EditableWorkflow.class)
                .orElseThrow(() -> new MethodNotAllowed(
                        withDisplayName(new ErrorInfo(Reason.NOT_A_DOCUMENT), handle)
                ));
    }
}
