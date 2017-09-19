/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.dialog.ConfigProvider;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogBehavior;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.addon.frontend.gallerypicker.ImageItem;
import org.onehippo.addon.frontend.gallerypicker.ImageItemFactory;
import org.onehippo.addon.frontend.gallerypicker.WicketJcrSessionProvider;
import org.onehippo.addon.frontend.gallerypicker.dialog.GalleryPickerDialog;

/**
 * Manages the picker dialog for imagelink fields. The dialog is used to select an image.
 * The behavior can be called by the frontend to open the dialog.
 * When done the method 'ChannelEditor#onImagePicked' is called.
 * Cancelling the dialog calls 'ChannelEditor#onImagePickCancelled'.
 */
class ImagePickerManager extends PickerManager<String> {

    private static final ImageItemFactory IMAGE_ITEM_FACTORY = new ImageItemFactory();

    private Model<String> dialogModel;

    ImagePickerManager(final IPluginContext context, final String channelEditorId) {
        super(context, channelEditorId);
        dialogModel = Model.of(StringUtils.EMPTY);
    }

    @Override
    protected DialogBehavior<String> createBehavior(final IPluginContext context, final ConfigProvider configProvider) {
        return new GalleryPickerDialogBehavior(context, configProvider);
    }

    @Override
    protected String toJsString(final String pickedItem) {
        final ImageItem imageItem = IMAGE_ITEM_FACTORY.createImageItem(pickedItem);
        final String url = imageItem.getPrimaryUrl(WicketJcrSessionProvider.get());

        return "{ uuid: '" + pickedItem + "', url: '" + url + "'}";
    }

    @Override
    protected void onConfigure(final IPluginConfig defaultDialogConfig, final Map<String, String> parameters) {
        dialogModel.setObject(parameters.get("uuid"));
    }

    private class GalleryPickerDialogBehavior extends DialogBehavior<String> {

        GalleryPickerDialogBehavior(final IPluginContext context, final ConfigProvider configProvider) {
            super(context, configProvider);
        }

        @Override
        protected Dialog<String> createDialog(final IPluginContext context, final IPluginConfig config) {
            return new GalleryPickerDialog(context, config, dialogModel);
        }

    }
}
