/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.skin.Icon;

public class EditablePropertyFieldContainer extends EditableFieldContainer {

    public EditablePropertyFieldContainer(final String id,
                                   final Item<IRenderService> renderItem,
                                   final JcrPropertyValueModel model,
                                   final PropertyFieldPlugin propertyField) {
        super(id, renderItem);

        final WebMarkupContainer controls = new WebMarkupContainer("controls");
        controls.setVisible(propertyField.canRemoveItem() || propertyField.canReorderItems());
        add(controls);

        MarkupContainer remove = new AjaxLink("remove") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                propertyField.onRemoveItem(model, target);
            }
        };
        if (!propertyField.canRemoveItem()) {
            remove.setVisible(false);
        }

        final HippoIcon removeIcon = HippoIcon.fromSprite("remove-icon", Icon.TIMES);
        remove.add(removeIcon);

        controls.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                propertyField.onMoveItemUp(model, target);
            }
        };
        boolean isFirst = (model.getIndex() == 0);
        if (!propertyField.canReorderItems()) {
            upLink.setVisible(false);
        }
        upLink.setEnabled(!isFirst);

        final HippoIcon upIcon = HippoIcon.fromSprite("up-icon", Icon.ARROW_UP);
        upLink.add(upIcon);

        controls.add(upLink);

        MarkupContainer downLink = new AjaxLink("down") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                JcrPropertyValueModel nextModel = new JcrPropertyValueModel(model.getIndex() + 1, model
                        .getJcrPropertymodel());
                propertyField.onMoveItemUp(nextModel, target);
            }
        };
        boolean isLast = (model.getIndex() == propertyField.provider.size() - 1);
        if (!propertyField.canReorderItems()) {
            downLink.setVisible(false);
        }
        downLink.setEnabled(!isLast);

        final HippoIcon downIcon = HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN);
        downLink.add(downIcon);

        controls.add(downLink);
    }
}
