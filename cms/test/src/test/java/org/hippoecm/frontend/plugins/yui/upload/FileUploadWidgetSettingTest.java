package org.hippoecm.frontend.plugins.yui.upload;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link FileUploadWidgetSettings}
 */
public class FileUploadWidgetSettingTest {

    @Test
    public void testDefaultUploadWidgetSetting() throws Exception {
        FileUploadWidgetSettings widgetSettings = new FileUploadWidgetSettings();

        assertFalse(widgetSettings.isAutoUpload());
        assertFalse(widgetSettings.isHideBrowseDuringUpload());
        assertFalse(widgetSettings.isClearAfterUpload());
        assertTrue(widgetSettings.isFlashUploadEnabled());
        assertTrue(widgetSettings.getFileExtensions().length == 0);
    }

    @Test
    public void testPluginConfigurationSettings() throws Exception {
        IPluginConfig pluginConfig = new JavaPluginConfig();

        pluginConfig.put("fileupload.flashEnabled","false");
        pluginConfig.put("fileupload.autoUpload","true");
        pluginConfig.put("fileupload.allowedExtensions",new String[]{".pdf",".jpg"});
        pluginConfig.put("fileupload.buttonWidth","122px");
        pluginConfig.put("fileupload.buttonHeight","50px");
        pluginConfig.put("fileupload.clearAfterUpload","true");
        pluginConfig.put("fileupload.clearTimeout","1500");
        pluginConfig.put("fileupload.hideBrowseDuringUpload","true");
        pluginConfig.put("fileupload.maxItems","15");

        FileUploadWidgetSettings widgetSettings = new FileUploadWidgetSettings(pluginConfig);

        assertTrue(widgetSettings.isAutoUpload());
        assertTrue(widgetSettings.isHideBrowseDuringUpload());
        assertTrue(widgetSettings.isClearAfterUpload());
        assertTrue(widgetSettings.getFileExtensions().length == 2);
        assertTrue(widgetSettings.getFileExtensions()[0].equals(".pdf"));
        assertTrue(widgetSettings.getMaxNumberOfFiles() == 15);
        assertTrue(widgetSettings.getClearTimeout() == 1500);
        assertTrue(widgetSettings.getButtonHeight().equals("50px"));
        assertTrue(widgetSettings.getButtonWidth().equals("122px"));

        assertFalse(widgetSettings.isFlashUploadEnabled());
    }

    @Test
    public void testFileExtensionsBackwardsCompatibility() throws Exception {
        IPluginConfig pluginConfig = new JavaPluginConfig();
        pluginConfig.put("file.extensions",new String[]{".doc",".pdf"});

        FileUploadWidgetSettings fileUploadWidgetSettings = new FileUploadWidgetSettings(pluginConfig);
        
        assertTrue(fileUploadWidgetSettings.getFileExtensions()[0].equals(".doc"));
        assertTrue(fileUploadWidgetSettings.getFileExtensions()[1].equals(".pdf"));
    }

}
