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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeException;

public class FieldEditor extends Panel {

    private static final long serialVersionUID = 1L;

    private String prefix;
    private ITypeDescriptor type;
    private boolean edit;

    public FieldEditor(String id, ITypeDescriptor type, IModel<IFieldDescriptor> model, boolean edit) {
        super(id, model);

        this.type = type;
        this.edit = edit;

        setOutputMarkupId(true);

        prefix = null;
        String typeName = type.getType();
        if (typeName.indexOf(':') > 0) {
            prefix = typeName.substring(0, typeName.indexOf(':'));
        }
        addFormField(new LockedTextField<String>("path", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            public String getObject() {
                String path = getDescriptor().getPath();
                if (path.indexOf(':') > 0 && path.startsWith(prefix)) {
                    return path.substring(prefix.length() + 1);
                }
                return path;
            }

            public void setObject(String path) {
                if (StringUtils.isBlank(path)) {
                    final StringResourceModel errorModel = new StringResourceModel("error-path-cannot-be-blank",
                            FieldEditor.this, null, new Object[]{getDescriptor().getName()});
                    showError(errorModel.getString());
                } else {
                    try {
                        if (path.indexOf(':') < 0) {
                            getDescriptor().setPath(prefix + ":" + path);
                        } else {
                            getDescriptor().setPath(path);
                        }
                    } catch (TypeException e) {
                        showError(e.getLocalizedMessage());
                    }
                }
            }

            private void showError(final String msg) {
                error(msg);
                AjaxRequestTarget target = AjaxRequestTarget.get();
                if (target != null) {
                    target.addComponent(FieldEditor.this);
                }
            }

            public void detach() {
            }
        }));
        // required checkbox
        CheckBox mandatoryCheckBox = new CheckBox("mandatory", new IModel<Boolean>() {
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
                if (getDescriptor().getValidators().contains("optional")) {
                    return false;
                }
                return super.isEnabled();
            }
        };
        addFormField(mandatoryCheckBox);
        Label mandatoryLabel = new Label("mandatory-label", new ResourceModel("mandatory"));
        mandatoryLabel.add(new CheckBoxDisableCssClassAppender(mandatoryCheckBox));
        add(mandatoryLabel);
        
        // optional checkbox
        CheckBox optionalCheckBox = new CheckBox("optional", new IModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            public Boolean getObject() {
                return getDescriptor().getValidators().contains("optional");
            }

            public void setObject(Boolean object) {
                IFieldDescriptor field = getDescriptor();
                if (object) {
                    field.addValidator("optional");
                } else {
                    field.removeValidator("optional");
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
                if (getDescriptor().getValidators().contains("required")) {
                    return false;
                }
                if (getDescriptor().isMultiple()) {
                    return false;
                }
                return super.isEnabled();
            }
        };
        addFormField(optionalCheckBox);
        Label optionalLabel = new Label("optional-label", new ResourceModel("optional"));
        optionalLabel.add(new CheckBoxDisableCssClassAppender(optionalCheckBox));
        add(optionalLabel);
        
        // multiple checkbox
        addFormField(new CheckBox("multiple", new IModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            public Boolean getObject() {
                return getDescriptor() == null ? null : new Boolean(getDescriptor().isMultiple());
            }

            public void setObject(Boolean object) {
                getDescriptor().setMultiple(object);
                if (object) {
                    getDescriptor().removeValidator("optional");
                }
            }

            public void detach() {
            }
        }));
        add(new Label("multiple-label", new ResourceModel("multiple")));
        
        // ordered checkbox
        CheckBox orderedCheckBox = new CheckBox("ordered", new IModel<Boolean>() {
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
        addFormField(orderedCheckBox);
        Label orderedLabel = new Label("ordered-label", new ResourceModel("ordered"));
        orderedLabel.add(new CheckBoxDisableCssClassAppender(orderedCheckBox));
        add(orderedLabel);
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
    
    class LockedTextField<T> extends TextField<T> implements IAjaxIndicatorAware {

        public LockedTextField(String id, IModel<T> tiModel) {
            super(id, tiModel);
        }

        @Override
        public String getAjaxIndicatorMarkupId() {
            return "veil";
        }
    }
    
    private static class CheckBoxDisableCssClassAppender extends CssClassAppender {

        private static final long serialVersionUID = 1L;

        public CheckBoxDisableCssClassAppender(final CheckBox checkBox) {
            super(new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public String load() {
                    if (checkBox.isEnabled()) {
                        return "";
                    } else {
                        return "disabled";
                    }
                }
            });
        }
        
    }
}
