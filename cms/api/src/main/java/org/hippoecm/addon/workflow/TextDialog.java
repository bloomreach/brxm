/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextDialog extends Dialog<Void> implements IWorkflowInvoker {

    private static final Logger log = LoggerFactory.getLogger(TextDialog.class);

    public TextDialog(final IModel<String> title, final IModel<String> question, final IModel<String> textModel) {
        super();
        setTitle(title);
        setSize(DialogConstants.MEDIUM);

        add(new Label("question", question));

        final TextAreaWidget textfield;
        add(textfield = new TextAreaWidget("value", textModel));
        textfield.addBehaviourOnFormComponent(ClassAttribute.append("text-dialog-textarea"));
        setFocus(textfield.getFocusComponent());
    }

    @Override
    protected void onOk() {
        try {
            invokeWorkflow();
        } catch (final Exception e) {
            log.error("Invoking workflow failed", e);
        }
    }
}
