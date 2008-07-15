/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.builder;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standardworkflow.types.IFieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;

public class FieldEditor extends Panel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id";

    private static final long serialVersionUID = 1L;

    private ITypeDescriptor type;

    public FieldEditor(String id, IModel model) {
        super(id, model);

        final IFieldDescriptor descriptor = (IFieldDescriptor) getModelObject();
        addFormField(new TextField("path", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return descriptor.getPath();
            }

            public void setObject(Object object) {
                descriptor.setPath((String) object);
            }

            public void detach() {
            }
        }));
        addFormField(new CheckBox("mandatory", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return descriptor == null ? null : new Boolean(descriptor.isMandatory());
            }

            public void setObject(Object object) {
                Boolean bool = (Boolean) object;
                descriptor.setMandatory(bool);
            }

            public void detach() {
            }
        }));
        addFormField(new CheckBox("multiple", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return descriptor == null ? null : new Boolean(descriptor.isMultiple());
            }

            public void setObject(Object object) {
                Boolean bool = (Boolean) object;
                descriptor.setMultiple(bool);
            }

            public void detach() {
            }
        }));
        addFormField(new CheckBox("ordered", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return descriptor == null ? null : new Boolean(descriptor.isOrdered());
            }

            public void setObject(Object object) {
                Boolean bool = (Boolean) object;
                descriptor.setOrdered(bool);
            }

            public void detach() {
            }
        }));
        addFormField(new CheckBox("primary", new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return descriptor == null ? null : new Boolean(descriptor.isPrimary());
            }

            public void setObject(Object object) {
                Boolean bool = (Boolean) object;
                if (bool) {
                    type.setPrimary(descriptor.getName());
                }
            }

            public void detach() {
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return type != null;
            }
        });
    }

    void setType(ITypeDescriptor type) {
        this.type = type;
    }

    @Override
    protected void onDetach() {
        if (type != null) {
            type.detach();
        }
        super.onDetach();
    }

    @Override
    public boolean isVisible() {
        return getModelObject() != null;
    }

    /**
     * Adds an ajax updating form component
     */
    protected void addFormField(FormComponent component) {
        add(component);
        component.setOutputMarkupId(true);
        component.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                FieldEditor.this.onUpdate(target);
            }
        });

    }

    protected void onUpdate(AjaxRequestTarget target) {
    }
}
