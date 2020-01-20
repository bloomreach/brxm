/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
        queue(controls);

        final AjaxLink<Void> remove = new AjaxLink<Void>("remove") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                propertyField.onRemoveItem(model, target);
            }
        };
        if (!propertyField.canRemoveItem()) {
            remove.setVisible(false);
        }
        queue(remove);

        final HippoIcon removeIcon = HippoIcon.fromSprite("remove-icon", Icon.TIMES);
        queue(removeIcon);

        final boolean isFirst = (model.getIndex() == 0);
        final AjaxLink<Void> upToTopLink = new AjaxLink<Void>("upToTop") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                propertyField.onMoveItemToTop(model);
                propertyField.redraw();
            }
        };
        upToTopLink.setVisible(propertyField.canReorderItems());
        upToTopLink.setEnabled(!isFirst);
        queue(upToTopLink);

        final HippoIcon upToTopIcon = HippoIcon.fromSprite("up-top-icon", Icon.ARROW_UP_LINE);
        queue(upToTopIcon);

        final AjaxLink<Void> upLink = new AjaxLink<Void>("up") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                propertyField.onMoveItemUp(model, target);
            }
        };
        if (!propertyField.canReorderItems()) {
            upLink.setVisible(false);
        }
        upLink.setEnabled(!isFirst);
        queue(upLink);

        final HippoIcon upIcon = HippoIcon.fromSprite("up-icon", Icon.ARROW_UP);
        queue(upIcon);

        final AjaxLink<Void> downLink = new AjaxLink<Void>("down") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final JcrPropertyValueModel nextModel = new JcrPropertyValueModel(model.getIndex() + 1, model
                        .getJcrPropertymodel());
                propertyField.onMoveItemUp(nextModel, target);
            }
        };
        final boolean isLast = (model.getIndex() == propertyField.provider.size() - 1);
        if (!propertyField.canReorderItems()) {
            downLink.setVisible(false);
        }
        downLink.setEnabled(!isLast);
        queue(downLink);

        final HippoIcon downIcon = HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN);
        queue(downIcon);

        final AjaxLink<Void> downToBottomLink = new AjaxLink<Void>("downToBottom") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                propertyField.onMoveItemToBottom(model);
                propertyField.redraw();
            }
        };
        downToBottomLink.setVisible(propertyField.canReorderItems());
        downToBottomLink.setEnabled(!isLast);
        queue(downToBottomLink);

        final HippoIcon downToBottomIcon = HippoIcon.fromSprite("down-bottom-icon", Icon.ARROW_DOWN_LINE);
        queue(downToBottomIcon);
    }
}
