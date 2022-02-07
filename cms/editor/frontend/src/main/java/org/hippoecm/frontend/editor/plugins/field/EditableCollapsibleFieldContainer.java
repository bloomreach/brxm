/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IRenderService;

public class EditableCollapsibleFieldContainer extends EditableNodeFieldContainer {

    public EditableCollapsibleFieldContainer(final String id,
                                             final Item<IRenderService> renderItem,
                                             final JcrNodeModel model,
                                             final NodeFieldPlugin nodeField,
                                             final boolean isCollapsed) {
        super(id, renderItem,model, nodeField, isCollapsed);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        get("field-title").setVisible(true);
    }

    @Override
    protected void onCollapse(final boolean isCollapsed) {
        super.onCollapse(isCollapsed);
    }
}
