/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.Date;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.FutureDateValidator;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.editor.workflow.dialog.ReferringDocumentsView;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.yui.datetime.YuiDateTimeField;
import org.hippoecm.frontend.service.IEditorManager;

public class ScheduleDepublishDialog extends AbstractWorkflowDialog {

    private static final long serialVersionUID = 1L;

    public ScheduleDepublishDialog(IWorkflowInvoker action, JcrNodeModel nodeModel, IModel<Date> dateModel, IEditorManager editorMgr) throws RepositoryException {
        super(nodeModel, action);

        ReferringDocumentsProvider provider = new ReferringDocumentsProvider(nodeModel, false);
        add(new ReferringDocumentsView("refs", provider, editorMgr));

        Calendar minimum = Calendar.getInstance();
        minimum.setTime(dateModel.getObject());
        minimum.set(Calendar.SECOND, 0);
        minimum.set(Calendar.MILLISECOND, 0);
        // if you want to round upwards, the following ought to be executed: minimum.add(Calendar.MINUTE, 1);
        dateModel.setObject(minimum.getTime());
        add(new Label("question", new ResourceModel("schedule-depublish-text")));
        YuiDateTimeField ydtf = new YuiDateTimeField("value", dateModel);
        ydtf.add(new FutureDateValidator());
        add(ydtf);
        setFocusOnCancel();
    }

    @Override
    public IModel getTitle() {
        return new StringResourceModel("schedule-depublish-title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return LARGE;
    }
}
