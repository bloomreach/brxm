/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

public abstract class AjaxUpdatingWidget<T> extends Panel {

    private FormComponent<T> formComponent;
    private Duration throttleDelay;

    public AjaxUpdatingWidget(final String id, final IModel<T> model) {
        super(id, model);
    }

    public AjaxUpdatingWidget(final String id, final IModel<T> model, final Duration throttleDelay) {
        super(id, model);
        this.throttleDelay = throttleDelay;
    }

    @SuppressWarnings("unchecked")
    public IModel<T> getModel() {
        return (IModel<T>) getDefaultModel();
    }

    public T getModelObject() {
        return getModel().getObject();
    }

    /**
     * Adds an ajax updating form component
     */
    protected void addFormField(final FormComponent<T> component) {
        add(formComponent = component);
        component.setOutputMarkupId(true);
        if(throttleDelay == null) {
            component.add(new AjaxFormComponentUpdatingBehavior("change") {

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    AjaxUpdatingWidget.this.onUpdate(target);
                }
            });
        } else {
            component.add(new OnChangeAjaxBehavior() {

                @Override
                protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setThrottlingSettings(new ThrottlingSettings(component.getMarkupId(), throttleDelay));
                }

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    AjaxUpdatingWidget.this.onUpdate(target);
                }

            });
        }
    }

    public Component getFocusComponent() {
        return getFormComponent();
    }

    public final FormComponent<T> getFormComponent() {
        return formComponent;
    }

    // callback for subclasses
    protected void onUpdate(final AjaxRequestTarget target) {
    }
}
