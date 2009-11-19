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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;

public class FieldEditor extends Panel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ITypeDescriptor type;
    private boolean edit;

    public FieldEditor(String id, ITypeDescriptor type, IModel<IFieldDescriptor> model, boolean edit) {
        super(id, model);

        this.type = type;
        this.edit = edit;

        addFormField(new TextField<String>("path", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            public String getObject() {
                return getDescriptor().getPath();
            }

            public void setObject(String object) {
                getDescriptor().setPath(object);
            }

            public void detach() {
            }
        }));
        addFormField(new CheckBox("mandatory", new IModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            public Boolean getObject() {
                return getDescriptor().getValidators().contains("required");
            }

            public void setObject(Boolean object) {
                IFieldDescriptor field = getDescriptor();
                if (object) {
                    field.addValidator("required");
                    if (field.getTypeDescriptor().isType("String")) {
                        field.addValidator("non-empty");
                    }
                } else {
                    if (field.getTypeDescriptor().isType("String")) {
                        field.removeValidator("non-empty");
                    }
                    field.removeValidator("required");
                }
            }

            public void detach() {
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                if (getDescriptor().isMandatory()) {
                    return false;
                }
                return super.isEnabled();
            }
        });
        addFormField(new CheckBox("multiple", new IModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            public Boolean getObject() {
                return getDescriptor() == null ? null : new Boolean(getDescriptor().isMultiple());
            }

            public void setObject(Boolean object) {
                getDescriptor().setMultiple(object);
            }

            public void detach() {
            }
        }));
        Label orderedLabel = new Label("ordered-label", new ResourceModel("ordered"));
        orderedLabel.add(new CssClassAppender(new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String load() {
                if(getDescriptor().isMultiple()) {
                    return "";
                } else {
                    return "disabled";
                }
            }
        }));
        add(orderedLabel);
        CheckBox ordered = new CheckBox("ordered", new IModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            public Boolean getObject() {
                return getDescriptor() == null ? null : new Boolean(getDescriptor().isOrdered());
            }

            public void setObject(Boolean object) {
                getDescriptor().setOrdered(object);
            }

            public void detach() {
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && getDescriptor().isMultiple();
            }
        };
        addFormField(ordered);
        addFormField(new CheckBox("primary", new IModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            public Boolean getObject() {
                return getDescriptor() == null ? null : new Boolean(getDescriptor().isPrimary());
            }

            public void setObject(Boolean object) {
                if (object) {
                    FieldEditor.this.type.setPrimary(getDescriptor().getName());
                }
            }

            public void detach() {
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return FieldEditor.this.type != null;
            }
        });
    }

    IFieldDescriptor getDescriptor() {
        return (IFieldDescriptor) getDefaultModelObject();
    }

    @Override
    public boolean isVisible() {
        return getDefaultModelObject() != null;
    }

    /**
     * Adds an ajax updating form component
     */
    protected void addFormField(FormComponent<?> component) {
        add(component);
        if (edit) {
            component.setOutputMarkupId(true);
            component.add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    FieldEditor.this.onUpdate(target);
                }
            });
        } else {
            component.setEnabled(false);
        }
    }

    protected void onUpdate(AjaxRequestTarget target) {
    }
}
