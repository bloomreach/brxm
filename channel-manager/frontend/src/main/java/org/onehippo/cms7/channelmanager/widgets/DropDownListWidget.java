/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.widgets;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.hst.core.parameters.DropDownList;

/**
 * Renders a widget to select an string value from a drop-down list. The given model is used to store the selected
 * string.
 */
public class DropDownListWidget extends Panel {

    private static final long serialVersionUID = 1L;

    public DropDownListWidget(final String id, final DropDownList dropDownList, final IModel<String> model, IChoiceRenderer<String> renderer) {
        super(id);

        List<String> options = Arrays.asList(dropDownList.value());
        final DropDownChoice<String> dropDown = new DropDownChoice("dropdownlist", model, options, renderer);

        if (options.contains(null)) {
            dropDown.setNullValid(true);
        }

        dropDown.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(DropDownListWidget.this);
            }
        });

        add(dropDown);
        setOutputMarkupId(true);
    }

}
