/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.reviewedactions.UnpublishedReferenceNodeProvider;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulePublishDialog extends WorkflowDialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(SchedulePublishDialog.class);


    public SchedulePublishDialog(final IWorkflowInvoker invoker, final IModel<Node> nodeModel,
                                 final IModel<Date> dateModel, final IModel<String> titleModel,
                                 final IModel<String> notification, final IEditorManager editorMgr) {
        super(invoker, nodeModel, titleModel);

        setCssClass("hippo-workflow-dialog");
        setNotification(notification);
        setOkLabel(getString("schedule-action"));
        setFocusOnOk();

        final Map<String, Node> referringDocuments = getReferringDocuments((JcrNodeModel) nodeModel);
        final UnpublishedReferenceNodeProvider provider = new UnpublishedReferenceNodeProvider(referringDocuments);
        add(new UnpublishedReferencesView("links", provider, editorMgr));

        addOrReplace(new DatePickerComponent(Dialog.BOTTOM_LEFT_ID, dateModel, new ResourceModel("schedule-publish-text")));
    }

    private static Map<String, Node> getReferringDocuments(final JcrNodeModel nodeModel) {
        try {
            return WorkflowUtils.getReferencesToUnpublishedDocuments(nodeModel.getNode(), UserSession.get().getJcrSession());
        } catch (RepositoryException e) {
            log.warn("Failed to get referring documents of '{}'", JcrUtils.getNodePathQuietly(nodeModel.getObject()), e);
        }
        return Collections.emptyMap();
    }
}
