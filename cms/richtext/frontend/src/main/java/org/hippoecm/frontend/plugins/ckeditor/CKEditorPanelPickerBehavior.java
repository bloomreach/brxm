/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.links.DocumentPickerBehavior;
import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.ckeditor.CKEditorConstants;

/**
 * Adds the CKEditor plugin 'hippopicker'.
 */
public class CKEditorPanelPickerBehavior implements CKEditorPanelBehavior {

    private final DocumentPickerBehavior documentPickerBehavior;
    private final ImagePickerBehavior imagePickerBehavior;

    public CKEditorPanelPickerBehavior(final DocumentPickerBehavior documentPickerBehavior, final ImagePickerBehavior imagePickerBehavior) {
        this.documentPickerBehavior = documentPickerBehavior;
        this.imagePickerBehavior = imagePickerBehavior;
    }

    @Override
    public void addCKEditorConfiguration(final JSONObject editorConfig) throws JSONException {
        JsonUtils.appendToCommaSeparatedString(editorConfig, CKEditorConstants.CONFIG_EXTRA_PLUGINS, HippoPicker.PLUGIN_NAME);

        final JSONObject pickerPluginConfig = JsonUtils.getOrCreateChildObject(editorConfig, HippoPicker.CONFIG_KEY);
        addInternalLinkPickerConfiguration(pickerPluginConfig);
        addImagePickerConfiguration(pickerPluginConfig);
    }

    private void addInternalLinkPickerConfiguration(final JSONObject pickerPluginConfig) throws JSONException {
        final JSONObject config = JsonUtils.getOrCreateChildObject(pickerPluginConfig, HippoPicker.InternalLink.CONFIG_KEY);
        addCallbackUrl(config, HippoPicker.InternalLink.CONFIG_CALLBACK_URL, documentPickerBehavior);
    }

    private void addImagePickerConfiguration(final JSONObject pickerPluginConfig) throws JSONException {
        final JSONObject config = JsonUtils.getOrCreateChildObject(pickerPluginConfig, HippoPicker.Image.CONFIG_KEY);
        addCallbackUrl(config, HippoPicker.Image.CONFIG_CALLBACK_URL, imagePickerBehavior);
    }

    private static void addCallbackUrl(final JSONObject config, String key, AbstractAjaxBehavior callback) throws JSONException {
        final String callbackUrl = callback.getCallbackUrl().toString();
        config.put(key, callbackUrl);
    }

    @Override
    public Iterable<AbstractAjaxBehavior> getAjaxBehaviors() {
        return Arrays.<AbstractAjaxBehavior>asList(documentPickerBehavior, imagePickerBehavior);
    }

    @Override
    public void detach() {
        // nothing to do
    }

}
