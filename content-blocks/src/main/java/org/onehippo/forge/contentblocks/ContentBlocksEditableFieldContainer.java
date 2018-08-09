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
package org.onehippo.forge.contentblocks;

import org.apache.wicket.MarkupContainer;
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
                                               final String blockName) {
        super(id, item, blockName);
        add(getControls(plugin, model, item));
    }

    private WebMarkupContainer getControls(final ContentBlocksFieldPlugin plugin, final JcrNodeModel model, 
                                           final Item<IRenderService> item) {
        WebMarkupContainer controls = new WebMarkupContainer("controls");
        controls.setVisible(plugin.canRemoveItem() || plugin.canReorderItems());

        // remove button
        MarkupContainer remove = new AjaxLink("remove") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = plugin.getPluginContext()
                        .getService(IDialogService.class.getName(), IDialogService.class);
                dialogService.show(new ContentBlocksEditableFieldContainer.DeleteItemDialog(model, plugin));
            }
        };
        remove.setVisible(plugin.canRemoveItem());
        controls.add(remove);

        final HippoIcon removeIcon = HippoIcon.fromSprite("remove-icon", Icon.TIMES);
        remove.add(removeIcon);

        final int itemIndex = item.getIndex();
        // up arrow button
        MarkupContainer upLink = new AjaxLink("up") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onMoveItemUp(model, target);
                plugin.redraw();
            }
        };
        upLink.setVisible(plugin.canReorderItems());
        upLink.setEnabled(itemIndex > 0);
        controls.add(upLink);

        final HippoIcon upIcon = HippoIcon.fromSprite("up-icon", Icon.ARROW_UP);
        upLink.add(upIcon);

        // down arrow button
        MarkupContainer downLink = new AjaxLink("down") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IFieldDescriptor field = plugin.getFieldHelper().getField();
                String name = field.getPath();
                JcrNodeModel parent = model.getParentModel();
                if (parent != null) {
                    String nextName = name + '[' + (itemIndex + 2) + ']';
                    String nextPath = parent.getItemModel().getPath() + '/' + nextName;
                    JcrNodeModel nextModel = new JcrNodeModel(nextPath);
                    plugin.onMoveItemUp(nextModel, target);
                    plugin.redraw();
                }
            }
        };

        downLink.setVisible(plugin.canReorderItems());
        downLink.setEnabled(itemIndex < plugin.getProvider().size() - 1);
        controls.add(downLink);

        final HippoIcon downIcon = HippoIcon.fromSprite("down-icon", Icon.ARROW_DOWN);
        downLink.add(downIcon);
        
        return controls;
    }
    
    private static class DeleteItemDialog extends Dialog<JcrNodeModel> {

        private final JcrNodeModel model;
        private final ContentBlocksFieldPlugin plugin;

        DeleteItemDialog(JcrNodeModel model, final ContentBlocksFieldPlugin plugin) {
            this.model = model;
            this.plugin = plugin;

            setFocusOnCancel();
            setSize(DialogConstants.SMALL);
            setTitleKey("delete-title");
        }

        @Override
        protected void onOk() {
            plugin.onRemoveItem(model, getRequestCycle().find(AjaxRequestTarget.class));
            plugin.redraw();
        }
    }
}
