/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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

import java.time.Duration;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

public class TextFieldWidget extends AjaxUpdatingWidget<String> {

    private String size;
    private String maxlength;

    public TextFieldWidget(String id, IModel<String> model) {
        this(id, model, null);
    }

    public TextFieldWidget(String id, IModel<String> model, IModel<String> labelModel) {
        this(id, model, labelModel, null);
    }

    public TextFieldWidget(String id, IModel<String> model, IModel<String> labelModel, Duration throttleDelay) {
        super(id, model, throttleDelay);

        final TextField<String> textField = new TextField<>("widget", model) {
            {
                setFlag(FLAG_CONVERT_EMPTY_INPUT_STRING_TO_NULL, false);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                if (getMaxlength() != null) {
                    tag.put("maxlength", getMaxlength());
                }
                if (getSize() != null) {
                    tag.put("size", getSize());
                }
                super.onComponentTag(tag);
            }
        };
        addFormField(textField);
        if (labelModel != null) {
           textField.setLabel(labelModel);
        }
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSize() {
        return size;
    }

    public String getMaxlength() {
        return maxlength;
    }

    public void setMaxlength(String maxlength) {
        this.maxlength = maxlength;
    }
}
