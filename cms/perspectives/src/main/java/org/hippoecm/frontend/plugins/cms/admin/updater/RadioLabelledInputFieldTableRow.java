/*
 * Copyright 2012-2023 Bloomreach
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.attributes.StyleAttribute;

public abstract class RadioLabelledInputFieldTableRow extends LabelledInputFieldTableRow {

    public RadioLabelledInputFieldTableRow(final String id, final RadioGroup<String> radios,
                                           final IModel<String> labelModel, final IModel<String> inputModel) {
        super(id, labelModel, inputModel);

        add(new Radio<String>("radio", new Model<>(id)) {
            @Override
            public boolean isVisible() {
                return isRadioVisible();
            }
        });

        label.add(StyleAttribute.set(() -> isRadioVisible()
                ? "padding-left: 5px;"
                : StringUtils.EMPTY));
    }

    protected abstract boolean isRadioVisible();
}
