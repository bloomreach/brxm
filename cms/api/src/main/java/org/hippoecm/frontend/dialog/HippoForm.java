/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.dialog;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.model.IModel;

/**
 * The HippoForm is used to manually clear old feedback messages prior processing because
 * {@code org.hippoecm.frontend.plugins.cms.root.RootPlugin#RootPlugin} configured to keep all feedback messages
 * after each request cycle.
 */
public class HippoForm<T> extends Form<T> {
    public HippoForm(final String id) {
        super(id);
    }

    public HippoForm(final String id, final IModel<T> model) {
        super(id, model);
    }

    @Override
    public void process(IFormSubmitter submittingComponent) {
        // clear feedbacks in form's data prior processing
        clearFeedbackMessages();

        super.process(submittingComponent);
    }

    public void clearFeedbackMessages(){
        if (hasFeedbackMessage()) {
            getFeedbackMessages().clear();
        }
    }
}
