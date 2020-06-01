/*
 * Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.selection.frontend.wicket;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;

/**
 * Radio group widget.
 *
 * @author Dennis Dam
 *
 */
public class RadioGroupWidget<T extends Serializable> extends Panel {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final CssResourceReference CSS = new CssResourceReference(RadioGroupWidget.class, "RadioGroupWidget.css");

    /**
     * Instantiates a new radio group widget.
     *
     * @param id
     *      The widget id
     * @param choices
     *      List containing {@link Radio} model objects
     * @param model the model
     *      Model that represents selected {@link Radio} item
     * @param verticalOrientation whether the radio items should be shown vertically (true) or
     *      horizontally (false)
     *
     */
    public RadioGroupWidget(String id, List<T> choices, IModel<T> model, boolean verticalOrientation) {
        super(id);

        final RadioGroup<T> group = new RadioGroup<T>("widget", model);
        group.setRenderBodyOnly(false);

        group.add(new ListView<T>("choices", choices) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<T> item) {

                final T radioitem = item.getModelObject();
                final Radio<T> radio = new Radio<T>("radio", Model.of(radioitem));

                radio.add(new AjaxEventBehavior("change") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        group.processInput();
                        onChange(target, group.getModelObject());
                    }

                    @Override
                    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);

                        attributes.setMethod(AjaxRequestAttributes.Method.POST);

                        final Map<String,Object> extraParameters = attributes.getExtraParameters();
                        extraParameters.put(group.getInputName(), radio.getValue());
                    }

                });
                item.add(radio);

                org.onehippo.forge.selection.frontend.model.ListItem valueListItem = (org.onehippo.forge.selection.frontend.model.ListItem) item
                        .getModelObject();
                radio.setLabel(getLabelModel(valueListItem.getLabel(), valueListItem.getKey()));
                item.add(new SimpleFormComponentLabel("label", radio));
            }
        });

        add(group);
        String cssClassDecorator = verticalOrientation ? "vertical" : "horizontal";
        group.add(new AttributeAppender("class", Model.of(cssClassDecorator), " "));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
    }

    /**
     * Override this method to handle the onChange event of the {@link RadioGroup}
     * @param target
     *          The request target
     * @param object Object held by the selected {@link Radio} component
     */
    protected void onChange(AjaxRequestTarget target, Object object) {
    }

    protected IModel<String> getLabelModel(final String defaultLabel, final String propertyValue) {
        return Model.of(defaultLabel);
    }

}
