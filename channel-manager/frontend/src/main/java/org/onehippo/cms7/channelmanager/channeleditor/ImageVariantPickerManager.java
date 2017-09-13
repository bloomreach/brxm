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

import javax.jcr.Node;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.CKEditorNodePlugin;
import org.hippoecm.frontend.plugins.richtext.dialog.AbstractRichTextEditorDialog;
import org.hippoecm.frontend.plugins.richtext.dialog.RichTextEditorAction;
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.images.RichTextEditorImageService;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketNodeFactory;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketURLEncoder;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImageFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImageFactoryImpl;

/**
 * Manages the picker dialog for images in rich text fields. The dialog is used to select an image and its variant.
 * The behavior can be called by the frontend to open the dialog.
 * When done the method 'ChannelEditor#onImageVariantPicked' is called.
 * Cancelling the dialog calls 'ChannelEditor#onImageVariantPickCancelled'.
 */
class ImageVariantPickerManager extends PickerManager {

    private final ImagePickerBehavior behavior;

    ImageVariantPickerManager(final IPluginContext context, final String channelEditorId) {
        super(CKEditorNodePlugin.DEFAULT_IMAGE_PICKER_CONFIG);

        final Model<Node> fieldNodeModel = getFieldNodeModel();
        final RichTextImageFactory imageFactory = new RichTextImageFactoryImpl(fieldNodeModel,
                WicketNodeFactory.INSTANCE, WicketURLEncoder.INSTANCE);
        final RichTextEditorImageService imageService = new RichTextEditorImageService(imageFactory);
        behavior = new StatelessImagePickerBehavior(context, getPickerConfig(), imageService);
        behavior.setCloseAction(new PickedAction<>(channelEditorId, "onImageVariantPicked", fieldNodeModel));
        behavior.setCancelAction((RichTextEditorAction<RichTextEditorImageLink>) richTextEditorImageLink ->
                String.format("Ext.getCmp('%s').%s();", channelEditorId, "onImageVariantPickCancelled"));
    }

    ImagePickerBehavior getBehavior() {
        return behavior;
    }

    private class StatelessImagePickerBehavior extends ImagePickerBehavior {

        StatelessImagePickerBehavior(final IPluginContext context,
                                     final IPluginConfig dialogConfig,
                                     final RichTextEditorImageService imageService) {
            super(context, dialogConfig, imageService);
        }

        @Override
        protected AbstractRichTextEditorDialog<RichTextEditorImageLink> createDialog() {
            initPicker(getParameters());
            return super.createDialog();
        }
    }
}
