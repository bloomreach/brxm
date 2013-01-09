/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfirmDialog extends AbstractDialog implements IWorkflowInvoker {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ConfirmDialog.class);

    private IModel<String> title;

    public ConfirmDialog(IModel<String> title, IModel<String> question) {
        this(title, null, null, question);
    }

    public ConfirmDialog(IModel<String> title, IModel<String> intro, IModel<String> text, IModel<String> question) {
        super();
        this.title = title;
        if (intro == null) {
            Label component;
            add(component = new Label("intro"));
            component.setVisible(false);
        } else {
            add(new Label("intro", intro));
        }
        if (text == null) {
            Label component;
            add(component = new Label("text"));
            component.setVisible(false);
        } else {
            add(new MultiLineLabel("text", text));
        }
        add(new Label("question", question));
    }

    @Override
    public IModel getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

    @Override
    protected void onOk() {
        try {
            invokeWorkflow();
        } catch (Exception e) {
            log.error("Could not execute workflow.", e);
        }
    }
}