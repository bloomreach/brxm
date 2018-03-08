/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.channelmanager.channeleditor;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.addon.frontend.gallerypicker.ImageItem;
import org.onehippo.addon.frontend.gallerypicker.ImageItemFactory;
import org.onehippo.addon.frontend.gallerypicker.dialog.GalleryPickerDialog;
import org.onehippo.cms.json.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Manages the picker dialog for imagelink fields. The dialog is used to select an image.
 */
class ImagePicker extends ChannelEditorPicker<String> {

    private static final ImageItemFactory IMAGE_ITEM_FACTORY = new ImageItemFactory();

    private final Model<String> dialogModel;

    ImagePicker(final IPluginContext context, final String channelEditorId) {
        super(context, null, channelEditorId);
        dialogModel = Model.of(StringUtils.EMPTY);
    }

    @Override
    protected DialogManager<String> createDialogManager(final IPluginContext context, final IPluginConfig config) {
        return new DialogManager<String>(context, config) {
            @Override
            protected Dialog<String> createDialog(final IPluginContext context, final IPluginConfig config, final Map<String, String> parameters) {
                return new GalleryPickerDialog(context, config, dialogModel);
            }

            @Override
            protected void beforeShowDialog(final Map<String, String> parameters) {
                dialogModel.setObject(parameters.get("uuid"));
            }
        };
    }

    @Override
    protected String toJson(final String pickedItem) {
        final ImageItem imageItem = IMAGE_ITEM_FACTORY.createImageItem(pickedItem);
        final String url = imageItem.getPrimaryUrl();

        final ObjectNode picked = Json.object();
        picked.put("uuid", pickedItem);
        picked.put("url", url);

        return picked.toString();
    }
}
