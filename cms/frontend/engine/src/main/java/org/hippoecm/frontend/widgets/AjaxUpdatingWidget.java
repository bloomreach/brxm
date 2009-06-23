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
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public abstract class AjaxUpdatingWidget extends Panel {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private FormComponent focus;

    public AjaxUpdatingWidget(String id, IModel model) {
        super(id, model);
    }

    /**
     * Adds an ajax updating form component
     */
    protected void addFormField(FormComponent component) {
        add(focus = component);
        component.setOutputMarkupId(true);
        component.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {
                    @Override
                    public CharSequence decorateScript(CharSequence script) {
                        return "Hippo.OnChangeTrigger.setOnChangeListener(null); " + super.decorateScript(script);
                    }
                };
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                AjaxUpdatingWidget.this.onUpdate(target);
            }

            @Override
            protected String getChannelName() {
                return "auc|s";
            }
        });
        component.add(new AbstractBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(Component component, ComponentTag tag) {
                tag.put("onfocus", "Hippo.OnChangeTrigger.setOnChangeListener('" + component.getMarkupId() + "');");
                tag.put("onblur", "Hippo.OnChangeTrigger.setOnChangeListener(null);");
            }
        });

    }

    public Component getFocusComponent() {
        return focus;
    }

    // callback for subclasses
    protected void onUpdate(AjaxRequestTarget target) {
    }
}
