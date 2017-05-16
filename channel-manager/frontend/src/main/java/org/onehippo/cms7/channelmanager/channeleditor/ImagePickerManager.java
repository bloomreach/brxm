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
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.images.RichTextEditorImageService;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;
import org.hippoecm.frontend.plugins.richtext.processor.WicketNodeFactory;
import org.hippoecm.frontend.plugins.richtext.processor.WicketURLEncoder;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImageFactory;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImageFactoryImpl;

/**
 * Manages the picker dialog for images in rich text fields. The behavior can be called by the frontend to
 * open the image picker. When done the method 'ChannelEditor#onImagePicked' is called.
 */
class ImagePickerManager extends PickerManager {

    private final ImagePickerBehavior behavior;

    ImagePickerManager(final IPluginContext context, final String channelEditorId) {
        super(CKEditorNodePlugin.DEFAULT_IMAGE_PICKER_CONFIG);

        final Model<Node> fieldNodeModel = getFieldNodeModel();
        final RichTextImageFactory imageFactory = new RichTextImageFactoryImpl(fieldNodeModel,
                WicketNodeFactory.INSTANCE, WicketURLEncoder.INSTANCE);
        final RichTextEditorImageService imageService = new RichTextEditorImageService(imageFactory);
        behavior = new StatelessImagePickerBehavior(context, getPickerConfig(), imageService);
        behavior.setCloseAction(new PickedAction<>(channelEditorId, "onImagePicked", fieldNodeModel));
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
