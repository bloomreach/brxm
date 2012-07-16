/*
 *  Copyright 2008-2012 Hippo.
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
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;

/**
 * Dialog for easy creating confirmation dialogs;
 */
public class DeleteDialog<T extends Serializable> extends AbstractDialog<T> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Component component;

    @SuppressWarnings({"unused"})
    public DeleteDialog() {
        add(new Label("label", new ResourceModel(getTextKey())));
        setFocusOnCancel();
    }

    public DeleteDialog(final IModel<T> model, final Component component) {
        super(model);
        this.component = component;
        add(new Label("label", new StringResourceModel(getTextKey(), component, model)));
        setFocusOnCancel();
    }

    public DeleteDialog(final T object, final Component component) {
        this(new Model<T>(object), component);
    }

    public IModel getTitle() {
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

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

}
