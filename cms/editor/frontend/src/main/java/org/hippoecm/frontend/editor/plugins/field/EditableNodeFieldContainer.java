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
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.IFieldDescriptor;

public class EditableNodeFieldContainer extends EditableFieldContainer {

    public EditableNodeFieldContainer(final String id,
                                      final Item<IRenderService> renderItem,
                                      final JcrNodeModel model,
                                      final NodeFieldPlugin nodeField) {
        this(id, renderItem, model, nodeField, false);
    }

    public EditableNodeFieldContainer(final String id, final Item<IRenderService> renderItem, final JcrNodeModel model, final NodeFieldPlugin nodeField, final boolean isCollapsed) {
        super(id, renderItem);

        final FieldPluginHelper helper = nodeField.helper;
        final IModel<String> captionModel = helper.getCaptionModel(this);
        final IModel<String> hintModel = helper.getHintModel(this);
        final boolean isRequired = helper.isRequired();

        final FieldTitle fieldTitle = helper.isCompoundField()
                ? new CollapsibleFieldTitle("field-title", captionModel, hintModel, isRequired, renderItem, isCollapsed) {
            @Override
            protected void onCollapse(final boolean isCollapsed) {
                EditableNodeFieldContainer.this.onCollapse(isCollapsed);
            }
        }
                : new FieldTitle("field-title", captionModel, hintModel, isRequired);

        queue(fieldTitle.setVisible(false));

        final int index = renderItem.getIndex();

        final WebMarkupContainer controls = new WebMarkupContainer("controls");
        controls.setVisible(nodeField.canRemoveItem() || nodeField.canReorderItems());
        queue(controls);

        final AjaxLink<Void> remove = new AjaxLink<Void>("remove") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                nodeField.onRemoveItem(model, target);
                nodeField.removeCollapsedItem(index);
            }
        };
        if (!nodeField.canRemoveItem()) {
            remove.setVisible(false);
        }
        queue(remove);

        final HippoIcon removeIcon = HippoIcon.fromSprite("remove-icon", Icon.TIMES);
        queue(removeIcon);

        final AjaxLink<Void> upToTopLink = new AjaxLink<Void>("upToTop") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                nodeField.onMoveItemToTop(model);
                nodeField.moveCollapsedItemToTop(index);
                nodeField.redraw();
            }
        };
        upToTopLink.setVisible(nodeField.canReorderItems());
        upToTopLink.setEnabled(index > 0);
        queue(upToTopLink);

        final HippoIcon upToTopIcon = HippoIcon.fromSprite("up-top-icon", Icon.ARROW_UP_LINE);
        queue(upToTopIcon);

        final AjaxLink<Void> upLink = new AjaxLink<Void>("up") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                nodeField.onMoveItemUp(model, target);
                nodeField.moveCollapsedItemUp(index);
            }
        };
        if (!nodeField.canReorderItems()) {
            upLink.setVisible(false);
        }
        if (index == 0) {
            upLink.setEnabled(false);
        }
        queue(upLink);

        final HippoIcon upIcon = HippoIcon.fromSprite("up-icon", Icon.ARROW_UP);
        queue(upIcon);

        final AjaxLink<Void> downLink = new AjaxLink<Void>("down") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IFieldDescriptor field = nodeField.getFieldHelper().getField();
                if (field == null) {
                    return;
                }

                final JcrNodeModel parent = model.getParentModel();
                if (parent == null) {
                    return;
                }

                final String parentPath = parent.getItemModel().getPath();
                final String nextPath = String.format("%s/%s[%d]", parentPath, field.getPath(), index + 2);
                final JcrNodeModel nextModel = new JcrNodeModel(nextPath);
                nodeField.onMoveItemUp(nextModel, target);
                nodeField.moveCollapsedItemDown(index);
            }
        };
        if (!nodeField.canReorderItems()) {
            downLink.setVisible(false);
        }
        final boolean isLast = (index == nodeField.provider.size() - 1);
        downLink.setEnabled(!isLast);
        queue(downLink);

        final HippoIcon downIcon = HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN);
        queue(downIcon);

        final AjaxLink<Void> downToBottomLink = new AjaxLink<Void>("downToBottom") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                nodeField.onMoveItemToBottom(model);
                nodeField.moveCollapsedItemToBottom(index);
                nodeField.redraw();
            }
        };
        downToBottomLink.setVisible(nodeField.canReorderItems());
        downToBottomLink.setEnabled(index < nodeField.provider.size() - 1);
        queue(downToBottomLink);

        final HippoIcon downToBottomIcon = HippoIcon.fromSprite("down-bottom-icon", Icon.ARROW_DOWN_LINE);
        queue(downToBottomIcon);
    }

    protected void onCollapse(final boolean isCollapsed) {
    }
}
