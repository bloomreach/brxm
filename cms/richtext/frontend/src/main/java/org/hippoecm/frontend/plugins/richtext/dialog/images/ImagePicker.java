/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.dialog.images;

import java.util.Map;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorImageLink;

public class ImagePicker extends DialogManager<RichTextEditorImageLink> {

    private final RichTextEditorImageService imageService;

    public ImagePicker(final IPluginContext context, final IPluginConfig config, final RichTextEditorImageService imageService) {
        super(context, config);
        this.imageService = imageService;
    }

    @Override
    protected Dialog<RichTextEditorImageLink> createDialog(final IPluginContext context, final IPluginConfig config, final Map<String, String> parameters) {
        final RichTextEditorImageLink imageLink = imageService.createRichTextEditorImage(parameters);
        final IModel<RichTextEditorImageLink> model = Model.of(imageLink);
        return new ImageBrowserDialog(context, config, model);
    }

    @Override
    public void detach() {
        if (imageService != null) {
            imageService.detach();
        }
    }
}
