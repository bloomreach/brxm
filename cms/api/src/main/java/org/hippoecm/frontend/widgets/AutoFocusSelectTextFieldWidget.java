/*
 *  Copyright 2014-2022 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;

public class AutoFocusSelectTextFieldWidget extends TextFieldWidget {
    public AutoFocusSelectTextFieldWidget(final String id, final IModel<String> model) {
        super(id, model);
    }

    public AutoFocusSelectTextFieldWidget(final String id, final IModel<String> model, final IModel<String> labelModel) {
        super(id, model, labelModel);
    }

    public AutoFocusSelectTextFieldWidget(final String id, final IModel<String> model, final IModel<String> labelModel, final Duration throttleDelay) {
        super(id, model, labelModel, throttleDelay);
    }

    /**
     * Add javascript to set focus to the component returned by {@code #getFocusComponent} 
     * and select the text when dialog is rendered.
     *
     * @param response header response
     */
    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        final String fieldMarkupId = getFocusComponent().getMarkupId();
        final String script = String.format(
                "document.getElementById('%s').focus();" +
                "document.getElementById('%s').select();",
                fieldMarkupId, fieldMarkupId);

        response.render(OnDomReadyHeaderItem.forScript(script));
    }

}
