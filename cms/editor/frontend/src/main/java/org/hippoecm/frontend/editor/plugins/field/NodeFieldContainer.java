/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.service.IRenderService;

public class NodeFieldContainer extends FieldContainer {
    public NodeFieldContainer(final String id, final Item<IRenderService> renderItem, final NodeFieldPlugin nodeField) {
        super(id, renderItem);

        final FieldPluginHelper helper = nodeField.helper;
        final IModel<String> captionModel = helper.getCaptionModel(this);
        final IModel<String> hintModel = helper.getHintModel(this);
        final boolean isRequired = helper.isRequired();

        final FieldTitle fieldTitle = helper.isCompoundField()
            ? new CollapsibleFieldTitle("field-title", captionModel, hintModel, isRequired, renderItem)
            : new FieldTitle("field-title", captionModel, hintModel, isRequired);

        queue(fieldTitle);
    }
}
