/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.dialog;

import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractWizard;
import org.hippoecm.frontend.editor.workflow.action.Action;

public abstract class CreateTypeDialog extends AbstractWizard {

    private static final long serialVersionUID = 1L;

    private Action action;
    private FeedbackPanel feedback;

    public CreateTypeDialog(Action action) {
        this.action = action;
        final IFeedbackMessageFilter[] filters = new IFeedbackMessageFilter[2];
        filters[0] = new ContainerFeedbackMessageFilter(action);
        filters[1] = new ContainerFeedbackMessageFilter(this);
        this.feedback = new FeedbackPanel(FEEDBACK_ID, new IFeedbackMessageFilter() {
            private static final long serialVersionUID = 1L;

            public boolean accept(FeedbackMessage message) {
                for (IFeedbackMessageFilter filter : filters) {
                    if (filter.accept(message)) {
                        return true;
                    }
                }
                return false;
            }
        });
        feedback.setOutputMarkupId(true);

        setOutputMarkupId(true);
    }

    @Override
    protected FeedbackPanel newFeedbackPanel(String id) {
        return feedback;
    }

    @Override
    public void onFinish() {
        // Important: first call super so the dialog can validate errors.
        // The action will remove this dialog from the page, after which the
        // error validation will fail (see CMS7-7357)
        super.onFinish();
        action.execute();
    }

    public IValueMap getProperties() {
        return new ValueMap("width=500,height=325");
    }

}
