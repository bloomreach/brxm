/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import java.io.IOException;
import java.util.Arrays;

import org.apache.wicket.behavior.Behavior;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePickerManager;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPickerManager;
import org.onehippo.ckeditor.HippoPicker;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Adds the CKEditor plugin 'hippopicker'.
 */
public class CKEditorPanelPickerExtension implements CKEditorPanelExtension {

    private final LinkPickerManager linkPicker;
    private final ImagePickerManager imagePicker;

    public CKEditorPanelPickerExtension(final LinkPickerManager linkPicker, final ImagePickerManager imagePicker) {
        this.linkPicker= linkPicker;
        this.imagePicker= imagePicker;
    }

    @Override
    public void addConfiguration(final ObjectNode editorConfig) throws IOException {
        final ObjectNode pickerPluginConfig = editorConfig.with(HippoPicker.CONFIG_KEY);
        addInternalLinkPickerConfiguration(pickerPluginConfig);
        addImagePickerConfiguration(pickerPluginConfig);
    }

    private void addInternalLinkPickerConfiguration(final ObjectNode pickerPluginConfig) {
        final ObjectNode config = pickerPluginConfig.with(HippoPicker.InternalLink.CONFIG_KEY);
        addCallbackUrl(config, HippoPicker.InternalLink.CONFIG_CALLBACK_URL, linkPicker);
    }

    private void addImagePickerConfiguration(final ObjectNode pickerPluginConfig) {
        final ObjectNode config = pickerPluginConfig.with(HippoPicker.Image.CONFIG_KEY);
        addCallbackUrl(config, HippoPicker.Image.CONFIG_CALLBACK_URL, imagePicker);
    }

    private static void addCallbackUrl(final ObjectNode config, final String key, final DialogManager provider) {
        final String callbackUrl = provider.getBehavior().getCallbackUrl().toString();
        config.put(key, callbackUrl);
    }

    @Override
    public Iterable<Behavior> getBehaviors() {
        return Arrays.asList(linkPicker.getBehavior(), imagePicker.getBehavior());
    }

    @Override
    public void detach() {
        linkPicker.detach();
        imagePicker.detach();
    }
}
