/*
 *  Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.fieldhint;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;

public class FieldHint extends Panel {
    private static final String JS_TEMPLATE = "$('#%s').tooltip({ container: 'body', placement: 'auto' })";
    private Component hint;

    public FieldHint(String id, final IModel<String> model) {
        super(id);

        hint = createHint(model);
        add(hint);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        final String script = String.format(JS_TEMPLATE, hint.getMarkupId());
        response.render(OnDomReadyHeaderItem.forScript(script));
    }

    /**
     * Create the hint component.
     * 
     * @param model The hint model holding the message
     * @return Hint container
     */
    protected Component createHint(final IModel<String> model) {
        final WebMarkupContainer container = new WebMarkupContainer("hint-visual");
        if (model == null) {
            return container.setVisible(false);
        }

        container.setOutputMarkupId(true);
        container.add(HippoIcon.fromSprite("hint-image", Icon.INFO_CIRCLE));
        container.add(new AttributeAppender("title", model));

        return container;
    }
}
