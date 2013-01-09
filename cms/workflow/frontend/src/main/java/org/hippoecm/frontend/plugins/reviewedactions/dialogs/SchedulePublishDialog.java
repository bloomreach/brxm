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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.FutureDateValidator;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.reviewedactions.UnpublishedReferenceNodeProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.ReferenceProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.UnpublishedReferenceProvider;
import org.hippoecm.frontend.plugins.yui.datetime.YuiDateTimeField;
import org.hippoecm.frontend.service.IEditorManager;

public class SchedulePublishDialog extends AbstractWorkflowDialog {

    private static final long serialVersionUID = 1L;

    public SchedulePublishDialog(IWorkflowInvoker action, JcrNodeModel nodeModel, IModel<Date> dateModel,
            IEditorManager editorMgr) {
        super(nodeModel, action);

        UnpublishedReferenceNodeProvider provider = new UnpublishedReferenceNodeProvider(
                new UnpublishedReferenceProvider(new ReferenceProvider(nodeModel)));
        add(new UnpublishedReferencesView("links", provider, editorMgr));

        Calendar minimum = Calendar.getInstance();
        minimum.setTime(dateModel.getObject());
        minimum.set(Calendar.SECOND, 0);
        minimum.set(Calendar.MILLISECOND, 0);
        // if you want to round upwards, the following ought to be executed: minimum.add(Calendar.MINUTE, 1);
        dateModel.setObject(minimum.getTime());
        add(new Label("question", new ResourceModel("schedule-publish-text")));
        YuiDateTimeField ydtf = new YuiDateTimeField("value", dateModel);
        ydtf.add(new FutureDateValidator());
        add(ydtf);
        setFocusOnCancel();
    }

    @Override
    public IModel getTitle() {
        return new StringResourceModel("schedule-publish-title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return LARGE;
    }
}
