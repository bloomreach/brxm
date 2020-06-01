/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.DefaultCssAutoCompleteTextField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

public abstract class AutoCompleteTextFieldWidget<T> extends AutoCompleteTextField<T> {

    private static final ResourceReference CSS = new CssResourceReference(DefaultCssAutoCompleteTextField.class,
            "DefaultCssAutoCompleteTextField.css");

    public AutoCompleteTextFieldWidget(final String id, final IModel<T> model, final AutoCompleteSettings settings) {
        super(id, model, settings);

        add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                AutoCompleteTextFieldWidget.this.onUpdate(target);
            }
        });
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
    }

    protected void onUpdate(final AjaxRequestTarget target) {
    }

    @Override
    protected void onRemove() {
        final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            final String containerId = getMarkupId() + "-autocomplete-container";
            // Remove element and add a second check after 1 second which ensures the element is also removed if
            // the AutoCompleteTextFieldWidget is used in a dialog that is closed by pressing ESC while the
            // AutoComplete is starting to render.
            final String js = String.format(
                    "if (jQuery) { " +
                        "jQuery('#%s').remove(); " +
                        "window.setTimeout(function() { " +
                            "jQuery('#%s').remove() " +
                        "}, 1000); " +
                    "}", containerId, containerId);
            target.getHeaderResponse().render(OnLoadHeaderItem.forScript(js));
        }
        super.onRemove();
    }


}
