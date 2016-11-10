/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.widgets;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;

public class DeleteDialog<T extends Serializable> extends Dialog<T> {

    private Component component;

    @SuppressWarnings({"unused"})
    public DeleteDialog() {
        init();
    }

    public DeleteDialog(final IModel<T> model, final Component component) {
        super(model);
        this.component = component;
        init();
    }

    public DeleteDialog(final T object, final Component component) {
        this(Model.of(object), component);
    }

    public IModel<String> getTitle() {
        if (component == null) {
            return new ResourceModel(getTitleKey());
        } else {
            return new StringResourceModel(getTitleKey(), component, getModel());
        }
    }

    protected String getTitleKey() {
        return "confirm-delete-title";
    }

    protected String getTextKey() {
        return "confirm-delete-text";
    }

    private void init() {
        setFocusOnCancel();
        setSize(DialogConstants.SMALL);

        add(new Label("label", getLabelModel()));
    }

    private IModel<String> getLabelModel() {
        if (component == null) {
            return new ResourceModel(getTextKey());
        } else {
            return new StringResourceModel(getTextKey(), component, getModel());
        }
    }

}
