/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeException;
import org.hippoecm.repository.api.HippoNodeType;

public class FieldEditor extends Panel {

    private String prefix;
    private final ITypeDescriptor type;
    private final boolean edit;

    public FieldEditor(final String id, final ITypeDescriptor type, final IModel<IFieldDescriptor> model, final boolean edit) {
        super(id, model);

        this.type = type;
        this.edit = edit;

        setOutputMarkupId(true);

        prefix = null;
        final String typeName = type.getType();
        if (typeName.indexOf(':') > 0) {
            prefix = typeName.substring(0, typeName.indexOf(':'));
        }
        addFormField(new LockedTextField<>("path", new IModel<String>() {

            @Override
            public String getObject() {
                final String path = getDescriptor().getPath();
                if (path.indexOf(':') > 0 && path.startsWith(prefix)) {
                    return path.substring(prefix.length() + 1);
                }
                return path;
            }

            @Override
            public void setObject(final String path) {
                if (StringUtils.isBlank(path)) {
                    final StringResourceModel errorModel = new StringResourceModel("error-path-cannot-be-blank",
                            FieldEditor.this, null, null, getDescriptor().getName());
                    showError(errorModel.getString());
                } else {
                    try {
                        if (path.indexOf(':') < 0) {
                            getDescriptor().setPath(prefix + ":" + path);
                        } else {
                            getDescriptor().setPath(path);
                        }
                    } catch (final TypeException e) {
                        showError(e.getLocalizedMessage());
                    }
                }
            }

            private void showError(final String msg) {
                error(msg);
                final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target != null) {
                    target.add(FieldEditor.this);
                }
            }

            @Override
            public void detach() {
            }
        }));

        // required checkbox
        final CheckBox mandatoryCheckBox = new CheckBox("mandatory", new IModel<Boolean>() {

            @Override
            public Boolean getObject() {
                return getDescriptor().getValidators().contains("required");
            }

            @Override
            public void setObject(final Boolean isRequired) {
                final IFieldDescriptor field = getDescriptor();
                final ITypeDescriptor typeDescriptor = field.getTypeDescriptor();
                final String validatorDescription = typeDescriptor.isType(HippoNodeType.NT_RESOURCE) ? "resource-required" : "required";
                if (isRequired) {
                    field.addValidator(validatorDescription);
                    if (typeDescriptor.isType("String")) {
                        field.addValidator("non-empty");
                    }
                } else {
                    if (typeDescriptor.isType("String")) {
                        field.removeValidator("non-empty");
                    }
                    field.removeValidator(validatorDescription);
                }
            }

            @Override
            public void detach() {
            }
        }) {
            @Override
            public boolean isEnabled() {
                final IFieldDescriptor descriptor = getDescriptor();
                if (descriptor.isMandatory()) {
                    return false;
                }
                if (descriptor.getValidators().contains("optional")) {
                    return false;
                }
                return super.isEnabled();
            }
        };
        addFormField(mandatoryCheckBox);
        final Label mandatoryLabel = new Label("mandatory-label", new ResourceModel("mandatory"));
        addCheckBoxCssClass(mandatoryLabel, mandatoryCheckBox);
        add(mandatoryLabel);

        // optional checkbox
        final CheckBox optionalCheckBox = new CheckBox("optional", new IModel<Boolean>() {

            @Override
            public Boolean getObject() {
                return getDescriptor().getValidators().contains("optional");
            }

            @Override
            public void setObject(final Boolean object) {
                final IFieldDescriptor field = getDescriptor();
                if (object) {
                    field.addValidator("optional");
                } else {
                    field.removeValidator("optional");
                }
            }

            @Override
            public void detach() {
            }
        }) {
            @Override
            public boolean isEnabled() {
                final IFieldDescriptor descriptor = getDescriptor();
                if (descriptor.isMandatory()) {
                    return false;
                }
                if (descriptor.getValidators().contains("required")) {
                    return false;
                }
                if (descriptor.isMultiple()) {
                    return false;
                }
                return super.isEnabled();
            }
        };
        addFormField(optionalCheckBox);
        final Label optionalLabel = new Label("optional-label", new ResourceModel("optional"));
        addCheckBoxCssClass(optionalLabel, optionalCheckBox);
        add(optionalLabel);

        // multiple checkbox
        addFormField(new CheckBox("multiple", new IModel<Boolean>() {

            @Override
            public Boolean getObject() {
                return getDescriptor() == null ? null : getDescriptor().isMultiple();
            }

            @Override
            public void setObject(final Boolean object) {
                getDescriptor().setMultiple(object);
                if (object) {
                    getDescriptor().removeValidator("optional");
                }
            }

            @Override
            public void detach() {
            }
        }));
        add(new Label("multiple-label", new ResourceModel("multiple")));

        // ordered checkbox
        final CheckBox orderedCheckBox = new CheckBox("ordered", new IModel<Boolean>() {

            @Override
            public Boolean getObject() {
                return getDescriptor() == null ? null : getDescriptor().isOrdered();
            }

            @Override
            public void setObject(final Boolean object) {
                getDescriptor().setOrdered(object);
            }

            @Override
            public void detach() {
            }
        }) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && getDescriptor().isMultiple();
            }
        };
        addFormField(orderedCheckBox);
        final Label orderedLabel = new Label("ordered-label", new ResourceModel("ordered"));
        addCheckBoxCssClass(orderedLabel, orderedCheckBox);
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
    protected void addFormField(final FormComponent<?> component) {
        add(component);
        if (edit) {
            component.setOutputMarkupId(true);
            component.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    FieldEditor.this.onUpdate(target);
                }
            });
        } else {
            component.setEnabled(false);
        }
    }

    protected void onUpdate(final AjaxRequestTarget target) {
    }

    private void addCheckBoxCssClass(final Label label, final CheckBox checkBox) {
        label.add(CssClass.append(ReadOnlyModel.of(() -> checkBox.isEnabled() ? "" : "disabled")));
    }

    class LockedTextField<T> extends TextField<T> implements IAjaxIndicatorAware {

        public LockedTextField(final String id, final IModel<T> tiModel) {
            super(id, tiModel);
        }

        @Override
        public String getAjaxIndicatorMarkupId() {
            return "veil";
        }
    }
}
