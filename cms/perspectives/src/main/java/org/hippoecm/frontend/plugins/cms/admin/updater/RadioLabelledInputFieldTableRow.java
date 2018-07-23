/**
 * Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class RadioLabelledInputFieldTableRow extends LabelledInputFieldTableRow {

    public RadioLabelledInputFieldTableRow(String id, RadioGroup<String> radios, final IModel<String> labelModel, final IModel<String> inputModel) {
        super(id, labelModel, inputModel);
        add(new Radio<String>("radio", new Model<String>(id)) {
            @Override
            public boolean isVisible() {
                return isRadioVisible();
            }
        });

        label.add(new AttributeModifier("style", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                if (isRadioVisible()) {
                    return "padding-left: 5px;";
                }
                return "";
            }
        }));
    }

    protected abstract boolean isRadioVisible();
}
