package org.hippoecm.frontend.plugins.ckeditor;

import java.util.Arrays;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.dialog.images.CKEditorImageService;
import org.hippoecm.frontend.plugins.ckeditor.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.ckeditor.dialog.links.CKEditorLinkService;
import org.hippoecm.frontend.plugins.ckeditor.dialog.links.DocumentPickerBehavior;
import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
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
        final JSONObject config = JsonUtils.getOrCreateChildObject(pickerPluginConfig, HippoPicker.INTERNAL_LINK_PICKER_CONFIG_KEY);
        addCallbackUrl(config, HippoPicker.INTERNAL_LINK_PICKER_CONFIG_CALLBACK_URL, documentPickerBehavior);
    }

    private void addImagePickerConfiguration(final JSONObject pickerPluginConfig) throws JSONException {
        final JSONObject config = JsonUtils.getOrCreateChildObject(pickerPluginConfig, HippoPicker.IMAGE_PICKER_CONFIG_KEY);
        addCallbackUrl(config, HippoPicker.IMAGE_PICKER_CONFIG_CALLBACK_URL, imagePickerBehavior);
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
