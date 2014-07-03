/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Settings for file uploads. Currently allowed configurable settings are:
 * <ul>
 *     <li>fileupload.flashEnabled = <code>true</code> for flash or <code>false</code> for javascript upload</li>
 *     <li>fileupload.maxItems = maximum allowed file uploads at the same time</li>
 *     <li>fileupload.allowedExtensions = allowed upload file extensions</li>
 *     <li>fileupload.autoUpload = if <code>true</code> the plugin will automatically upload the files</li>
 *     <li>fileupload.buttonWidth = defines the width of the upload button</li>
 *     <li>fileupload.buttonHeight = defines the height of the upload button</li>
 *     <li>fileupload.clearAfterUpload = if <code>true</code> the dialog is cleared after all files are uploaded</li>
 *     <li>fileupload.clearTimeout = defines the timeout before clearing the dialog after the upload</li>
 *     <li>fileupload.hideBrowseDuringUpload = if <code>true</code> the browse button will be hidden during the upload</li>
 *     <li>fileupload.concurrentUploads = maximum allowed concurrent file uploads</li>
 * </ul>
 * Backwards compatibility:
 * <ul>
 *     <li>file.extensions = allowed upload file extensions</li>
 * </ul>
 */
public class FileUploadWidgetSettings implements IClusterable {

    public static final String FILEUPLOAD_FLASH_ENABLED_SETTING = "fileupload.flashEnabled";
    public static final String FILEUPLOAD_MAX_ITEMS_SETTING = "fileupload.maxItems";
    public static final String FILEUPLOAD_AUTOUPLOAD_SETTING = "fileupload.autoUpload";
    public static final String FILEUPLOAD_ALLOWED_EXTENSIONS_SETTING = "fileupload.allowedExtensions";
    public static final String FILEUPLOAD_BUTTON_WIDTH = "fileupload.buttonWidth";
    public static final String FILEUPLOAD_BUTTON_HEIGHT = "fileupload.buttonHeight";
    public static final String FILEUPLOAD_CLEAR_AFTER_UPLOAD = "fileupload.clearAfterUpload";
    public static final String FILEUPLOAD_CLEAR_TIMEOUT = "fileupload.clearTimeout";
    public static final String FILEUPLOAD_HIDE_BROWSE_DURING_UPLOAD = "fileupload.hideBrowseDuringUpload";
    public static final String FILEUPLOAD_CONCURRENT_UPLOADS = "fileupload.concurrentUploads";
    public static final String FILEUPLOAD_ALWAYS_SHOW_LABEL = "fileupload.alwaysShowLabel";
    public static final String FILEUPLOAD_USE_MULTIPLE_ATTR = "fileupload.useMultipleAttr";

    //backwards compatibility
    public static final String FILE_EXTENSIONS_SETTING = "file.extensions";

    private String[] fileExtensions = new String[0];
    private int maxNumberOfFiles = 1;
    private int simultaneousUploadLimit = 3;
    private boolean autoUpload;
    private boolean clearAfterUpload;
    private int clearTimeout = 1000;
    private boolean hideBrowseDuringUpload;
    private String buttonWidth;
    private String buttonHeight;
    private boolean flashUploadEnabled = true;
    private boolean alwaysShowLabel;
    private boolean useMultipleAttr = false;

    public FileUploadWidgetSettings() {
    }

    public FileUploadWidgetSettings(IPluginConfig pluginConfig) {
        parsePluginConfig(pluginConfig);
    }

    public void setFileExtensions(String[] fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setMaxNumberOfFiles(int nr) {
        maxNumberOfFiles = nr;
    }

    public int getMaxNumberOfFiles() {
        return maxNumberOfFiles;
    }

    public void setAutoUpload(boolean set) {
        autoUpload = set;
    }

    public boolean isAutoUpload() {
        return autoUpload;
    }

    public void setClearAfterUpload(boolean clear) {
        this.clearAfterUpload = clear;
    }

    public boolean isClearAfterUpload() {
        return clearAfterUpload;
    }

    public int getClearTimeout() {
        return clearTimeout;
    }

    public void setClearTimeout(int clearTimeout) {
        this.clearTimeout = clearTimeout;
    }

    public void setHideBrowseDuringUpload(boolean hideBrowseDuringUpload) {
        this.hideBrowseDuringUpload = hideBrowseDuringUpload;
    }

    public boolean isHideBrowseDuringUpload() {
        return hideBrowseDuringUpload;
    }

    public String getButtonWidth() {
        return buttonWidth;
    }

    public void setButtonWidth(String buttonWidth) {
        this.buttonWidth = buttonWidth;
    }

    public String getButtonHeight() {
        return buttonHeight;
    }

    public void setButtonHeight(String buttonHeight) {
        this.buttonHeight = buttonHeight;
    }

    /**
     * Indicates if the upload widget should use Flash.
     * @return <code>true</code> if flash should be used, <code>false</code> otherwise
     */
    public boolean isFlashUploadEnabled() {
        return flashUploadEnabled;
    }

    /**
     * If set to <code>true</code> (default) the upload plugin will use flash for file uploads, otherwise it will use a
     * plain Javascript upload.
     *
     * @param flashUploadEnabled boolean indicating if flash should be used for file uploads.
     */
    public void setFlashUploadEnabled(boolean flashUploadEnabled) {
        this.flashUploadEnabled = flashUploadEnabled;
    }

    public int getSimultaneousUploadLimit() {
        return simultaneousUploadLimit;
    }

    public void setSimultaneousUploadLimit(final int simultaneousUploadLimit) {
        this.simultaneousUploadLimit = simultaneousUploadLimit;
    }

    public boolean isAlwaysShowLabel() {
        return alwaysShowLabel;
    }

    public void setAlwaysShowLabel(final boolean alwaysShowLabel) {
        this.alwaysShowLabel = alwaysShowLabel;
    }

    public boolean isUseMultipleAttr() {
        return useMultipleAttr;
    }

    public void setUseMultipleAttr(final boolean useMultipleAttr) {
        this.useMultipleAttr = useMultipleAttr;
    }

    private void parsePluginConfig(final IPluginConfig pluginConfig) {

        if (pluginConfig.containsKey(FILEUPLOAD_FLASH_ENABLED_SETTING)) {
            this.flashUploadEnabled = pluginConfig.getAsBoolean(FILEUPLOAD_FLASH_ENABLED_SETTING);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_MAX_ITEMS_SETTING)) {
            this.maxNumberOfFiles = pluginConfig.getAsInteger(FILEUPLOAD_MAX_ITEMS_SETTING);
        }

        // for backwards compatibility
        if (pluginConfig.containsKey(FILE_EXTENSIONS_SETTING)) {
            this.fileExtensions = pluginConfig.getStringArray(FILE_EXTENSIONS_SETTING);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_ALLOWED_EXTENSIONS_SETTING)) {
            this.fileExtensions = pluginConfig.getStringArray(FILEUPLOAD_ALLOWED_EXTENSIONS_SETTING);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_AUTOUPLOAD_SETTING)) {
            this.autoUpload = pluginConfig.getAsBoolean(FILEUPLOAD_AUTOUPLOAD_SETTING);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_BUTTON_WIDTH)) {
            this.buttonWidth = pluginConfig.getString(FILEUPLOAD_BUTTON_WIDTH);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_BUTTON_HEIGHT)) {
            this.buttonHeight = pluginConfig.getString(FILEUPLOAD_BUTTON_HEIGHT);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_CLEAR_AFTER_UPLOAD)) {
            this.clearAfterUpload = pluginConfig.getAsBoolean(FILEUPLOAD_CLEAR_AFTER_UPLOAD);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_CLEAR_TIMEOUT)) {
            this.clearTimeout = pluginConfig.getAsInteger(FILEUPLOAD_CLEAR_TIMEOUT);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_HIDE_BROWSE_DURING_UPLOAD)) {
            this.hideBrowseDuringUpload = pluginConfig.getAsBoolean(FILEUPLOAD_HIDE_BROWSE_DURING_UPLOAD);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_CONCURRENT_UPLOADS)) {
            this.simultaneousUploadLimit = pluginConfig.getAsInteger(FILEUPLOAD_CONCURRENT_UPLOADS);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_ALWAYS_SHOW_LABEL)) {
            this.alwaysShowLabel = pluginConfig.getAsBoolean(FILEUPLOAD_ALWAYS_SHOW_LABEL);
        }

        if (pluginConfig.containsKey(FILEUPLOAD_USE_MULTIPLE_ATTR)) {
            this.useMultipleAttr = pluginConfig.getAsBoolean(FILEUPLOAD_USE_MULTIPLE_ATTR);
        }
    }
}
