/**
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.updater;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class LabelledDropDownFieldTableRow extends Panel {

    private static final long serialVersionUID = 1L;

    protected final Label label;
    protected final DropDownChoice<String> choice;

    public LabelledDropDownFieldTableRow(String id, final IModel<String> labelModel, final IModel<String> inputModel,
            final Map<String, String> choiceValueAndLabelsMap) {
        super(id);
        label = new Label("label", labelModel);
        add(label);
        final List<String> choiceValues = new ArrayList<>(choiceValueAndLabelsMap.keySet());
        choice = new DropDownChoice<String>("choice", inputModel, choiceValues, new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(String object) {
                return StringUtils.defaultIfBlank(choiceValueAndLabelsMap.get(object), object);
            }

            @Override
            public String getIdValue(String object, int index) {
                return object;
            }

            @Override
            public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                final List<? extends String> choices = choicesModel.getObject();
                return choices.contains(id) ? id : null;
            }
        });
        add(choice);
    }

}
