/*
 *  Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;

public class FieldHint extends Panel {

    public FieldHint(String id, final IModel<String> hintModel) {
        super(id);
        add(createHint(hintModel));
    }

    protected Component createHint(final IModel<String> hintModel) {
        final WebMarkupContainer hintContainer = new WebMarkupContainer("hint-visual");
        if (hintModel == null) {
            hintContainer.setVisible(false);
        } else {
            hintContainer.add(new Label("hint-text", hintModel));
            hintContainer.add(HippoIcon.fromSprite("hint-image", Icon.INFO_CIRCLE));
        }
        return hintContainer;
    }
}
