/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextDialog extends AbstractDialog implements IWorkflowInvoker {

    private static final Logger log = LoggerFactory.getLogger(TextDialog.class);

    private final IModel<String> title;

    public TextDialog(final IModel<String> title, final IModel<String> question, final IModel<String> textModel) {
        super();

        this.title = title;
        add(new Label("question", question));

        final TextAreaWidget textfield;
        add(textfield = new TextAreaWidget("value", textModel));
        textfield.addBehaviourOnFormComponent(CssClass.append("text-dialog-textarea"));
        setFocus(textfield.getFocusComponent());
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    protected void onOk() {
        try {
            invokeWorkflow();
        } catch (final Exception e) {
            log.error("Invoking workflow failed", e);
        }
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }
}
