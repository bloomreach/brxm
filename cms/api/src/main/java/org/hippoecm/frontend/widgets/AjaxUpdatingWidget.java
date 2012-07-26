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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

public abstract class AjaxUpdatingWidget<T> extends Panel {
    private static final long serialVersionUID = 1L;

    private FormComponent<? extends T> focus;
    private Duration throttleDelay;

    public AjaxUpdatingWidget(String id, IModel<T> model) {
        super(id, model);
    }

    public AjaxUpdatingWidget(String id, IModel<T> model, Duration throttleDelay) {
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
    protected void addFormField(FormComponent<? extends T> component) {
        add(focus = component);
        component.setOutputMarkupId(true);
        if(throttleDelay == null) {
            component.add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    AjaxUpdatingWidget.this.onUpdate(target);
                }
            });
        } else {
            component.add(new OnChangeAjaxBehavior() {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    AjaxUpdatingWidget.this.onUpdate(target);
                }

            }.setThrottleDelay(throttleDelay));

        }
    }

    public Component getFocusComponent() {
        return focus;
    }

    // callback for subclasses
    protected void onUpdate(AjaxRequestTarget target) {
    }
}
