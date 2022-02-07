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
package org.onehippo.forge.contentblocks;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.types.IFieldDescriptor;

public class ContentBlocksEditableFieldContainer extends ContentBlocksFieldContainer {

    public ContentBlocksEditableFieldContainer(final String id, final Item<IRenderService> item,
                                               final JcrNodeModel model, final ContentBlocksFieldPlugin plugin,
                                               final String blockName, final boolean isCollapsed) {
        super(id, item, blockName, isCollapsed);
        add(getControls(plugin, model, item));
    }

    private static WebMarkupContainer getControls(final ContentBlocksFieldPlugin plugin, final JcrNodeModel model,
                                                  final Item<IRenderService> item) {
        final WebMarkupContainer controls = new WebMarkupContainer("controls");
        controls.setVisible(plugin.canRemoveItem() || plugin.canReorderItems());

        final int itemIndex = item.getIndex();

        // remove button
        final AjaxLink<Void> remove = new AjaxLink<Void>("remove") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IDialogService dialogService = plugin.getPluginContext()
                        .getService(IDialogService.class.getName(), IDialogService.class);
                dialogService.show(new ContentBlocksEditableFieldContainer.DeleteItemDialog(model, plugin, itemIndex));
            }
        };
        remove.setVisible(plugin.canRemoveItem());
        controls.add(remove);

        final HippoIcon removeIcon = HippoIcon.fromSprite("remove-icon", Icon.TIMES);
        remove.add(removeIcon);

        // up to top arrow button
        final AjaxLink<Void> upToTopLink = new AjaxLink<Void>("upToTop") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                plugin.onMoveItemToTop(model);
                plugin.moveCollapsedItemToTop(itemIndex);
                plugin.redraw();
            }
        };
        upToTopLink.setVisible(plugin.canReorderItems());
        upToTopLink.setEnabled(itemIndex > 0);
        controls.add(upToTopLink);

        final HippoIcon upToTopIcon = HippoIcon.fromSprite("up-top-icon", Icon.ARROW_UP_LINE);
        upToTopLink.add(upToTopIcon);

        // up arrow button
        final AjaxLink<Void> upLink = new AjaxLink<Void>("up") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                plugin.onMoveItemUp(model, target);
                plugin.moveCollapsedItemUp(itemIndex);
                plugin.redraw();
            }
        };
        upLink.setVisible(plugin.canReorderItems());
        upLink.setEnabled(itemIndex > 0);
        controls.add(upLink);

        final HippoIcon upIcon = HippoIcon.fromSprite("up-icon", Icon.ARROW_UP);
        upLink.add(upIcon);

        // down arrow button
        final AjaxLink<Void> downLink = new AjaxLink<Void>("down") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IFieldDescriptor field = plugin.getFieldHelper().getField();
                final String name = field.getPath();
                final JcrNodeModel parent = model.getParentModel();
                if (parent != null) {
                    final String nextName = name + '[' + (itemIndex + 2) + ']';
                    final String nextPath = parent.getItemModel().getPath() + '/' + nextName;
                    final JcrNodeModel nextModel = new JcrNodeModel(nextPath);
                    plugin.onMoveItemUp(nextModel, target);
                    plugin.moveCollapsedItemDown(itemIndex);
                    plugin.redraw();
                }
            }
        };

        downLink.setVisible(plugin.canReorderItems());
        downLink.setEnabled(itemIndex < plugin.getProvider().size() - 1);
        controls.add(downLink);

        final HippoIcon downIcon = HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN);
        downLink.add(downIcon);

        // down to bottom arrow button
        final AjaxLink<Void> downToBottomLink = new AjaxLink<Void>("downToBottom") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                plugin.onMoveItemToBottom(model);
                plugin.moveCollapsedItemToBottom(itemIndex);
                plugin.redraw();
            }
        };

        downToBottomLink.setVisible(plugin.canReorderItems());
        downToBottomLink.setEnabled(itemIndex < plugin.getProvider().size() - 1);
        controls.add(downToBottomLink);

        final HippoIcon downToBottomIcon = HippoIcon.fromSprite("down-bottom-icon", Icon.ARROW_DOWN_LINE);
        downToBottomLink.add(downToBottomIcon);

        return controls;
    }

    private static class DeleteItemDialog extends Dialog<JcrNodeModel> {

        private final JcrNodeModel model;
        private final ContentBlocksFieldPlugin plugin;
        private final int itemIndex;

        DeleteItemDialog(final JcrNodeModel model, final ContentBlocksFieldPlugin plugin, final int itemIndex) {
            this.model = model;
            this.plugin = plugin;
            this.itemIndex = itemIndex;

            setFocusOnCancel();
            setSize(DialogConstants.SMALL);
            setTitleKey("delete-title");
        }

        @Override
        protected void onOk() {
            plugin.onRemoveItem(model, getRequestCycle().find(AjaxRequestTarget.class).orElse(null));
            plugin.removeCollapsedItem(itemIndex);
            plugin.redraw();
        }
    }
}
