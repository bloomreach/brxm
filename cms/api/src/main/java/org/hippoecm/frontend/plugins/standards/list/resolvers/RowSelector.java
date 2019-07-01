/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.event.IObservable;

public class RowSelector<T extends Serializable> implements IListCellRenderer<T> {

    private static final long serialVersionUID = 1L;

    private final List<T> selectedDocuments;

    public RowSelector(List<T> selectedDocuments) {
        this.selectedDocuments = selectedDocuments;
    }

    public Component getRenderer(String id, IModel<T> model) {
        return new CheckBoxWrapper(id, model);
    }

    private class CheckBoxWrapper extends Panel {
        public CheckBoxWrapper(String id, IModel<T> model) {
            super(id, model);

            CheckBox check = new CheckBox("check", new IModel<Boolean>() {
                @SuppressWarnings("unchecked")
                public Boolean getObject() {
                    return selectedDocuments.contains((T) CheckBoxWrapper.this.getDefaultModel());
                }

                @SuppressWarnings("unchecked")
                public void setObject(Boolean value) {
                    if (value) {
                        selectedDocuments.add((T) CheckBoxWrapper.this.getDefaultModel());
                    } else {
                        selectedDocuments.remove((T) CheckBoxWrapper.this.getDefaultModel());
                    }
                }

                public void detach() {
                    // do nothing
                }

            });
            check.add(new AjaxFormComponentUpdatingBehavior("click") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(CheckBoxWrapper.this);
                }
            });

            add(check);
            setOutputMarkupId(true);
        }

    }

    public IObservable getObservable(IModel<T> model) {
        return null;
    }

}
