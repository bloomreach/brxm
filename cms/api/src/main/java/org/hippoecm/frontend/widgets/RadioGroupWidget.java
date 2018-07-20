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

import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class RadioGroupWidget<T> extends Panel {
    private static final long serialVersionUID = 1L;


    /**
     * Instantiates a new radio group widget.
     * 
     * @param id 
     *      The widget id
     * @param choices 
     *      List containing {@link Radio} model objects
     * @param model the model
     *      Model that represents selected {@link Radio} item  
     *      
     */
    public RadioGroupWidget(final String id, final List<T> choices, final IModel<T> model) {
        super(id);

        final RadioGroup<T> group = new RadioGroup<T>("widget", model);
        group.setRenderBodyOnly(false);

        group.add(new ListView<T>("choices", choices) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<T> item) {

                final Radio<T> radio = new Radio<T>("radio", item.getModel());
                radio.add(new AjaxEventBehavior("onchange") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        group.processInput();
                        onChange(target, group.getModelObject());
                    }

                    /*
                    @Override
                    protected CharSequence getEventHandler() {
                        return generateCallbackScript(new AppendingStringBuffer("wicketAjaxPost('").append(
                                getCallbackUrl()).append(
                                "', wicketSerialize(document.getElementById('" + radio.getMarkupId() + "'))"));
                    }*/
                });
                item.add(radio);

                String label = item.getDefaultModelObjectAsString();
                radio.setLabel(new Model<String>(getLocalizer().getString(label, this, label)));
                item.add(new SimpleFormComponentLabel("label", radio));

                RadioGroupWidget.this.populateItem(item);
            }
        });

        add(group);
    }

    /**
     * Override this method to change the ListItem
     * 
     * @param item
     */
    protected void populateItem(ListItem<T> item) {
    }

    /**
     * Override this method to handle the onChange event of the {@link RadioGroup}
     * @param target
     *          The request target
     * @param object
     *          Object held by the selected {@link Radio} component 
     */
    protected void onChange(AjaxRequestTarget target, Object object) {
    }

}
