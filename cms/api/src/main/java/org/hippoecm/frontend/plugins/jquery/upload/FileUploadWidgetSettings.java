/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;

public class FileUploadWidgetSettings implements IClusterable {
    public static final String MAX_WIDTH_PROP = "max.width";
    public static final String MAX_HEIGHT_PROP = "max.height";
    public static final String MAX_FILESIZE_PROP = "max.file.size";
    public static final String FILEUPLOAD_MAX_ITEMS = "fileupload.maxItems";
    public static final String AUTOUPLOAD_PROP = "autoUpload";

    private static final long DEFAULT_MAX_NUMBER_OF_FILES = 25;

    private long maxWidth;
    private long maxHeight;
    private long maxFileSize;
    private String uploadUrl;
    private String uploadDoneNotificationUrl;
    private long maxNumberOfFiles;
    private String[] allowedExtensions;
    private boolean autoUpload;
    private String selectionChangeNotificationUrl;

    public FileUploadWidgetSettings(final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        loadConfig(pluginConfig, validator);
    }

    public void loadConfig(final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        this.allowedExtensions = validator.getAllowedExtensions();
        this.maxFileSize = validator.getMaxFileSize().bytes();

        this.maxWidth = pluginConfig.getAsLong(MAX_WIDTH_PROP, ImageUploadValidationService.DEFAULT_MAX_WIDTH);
        this.maxHeight = pluginConfig.getAsLong(MAX_HEIGHT_PROP, ImageUploadValidationService.DEFAULT_MAX_HEIGHT);
        this.maxNumberOfFiles = pluginConfig.getAsLong(FILEUPLOAD_MAX_ITEMS, DEFAULT_MAX_NUMBER_OF_FILES);
    }

    /**
     * Maximal width of allowed upload images.
     */
    public long getMaxWidth() {
        return maxWidth;
    }

    /**
     * Maximal height of allowed upload images.
     */
    public long getMaxHeight() {
        return maxHeight;
    }

    /**
     * Maximal file size of allowed upload files.
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * The url to receive uploading files. This setting is used for multi-files uploads only.
     */
    public String getUploadUrl() {
        return uploadUrl;
    }

    /**
     * The url to receive uploading files. This setting is used for multi-files uploads only.
     *
     * @param url
     */
    public void setUploadUrl(String url) {
        this.uploadUrl = url;
    }

    /**
     * The url to notify on the completion of uploading. This setting is used for multi-files uploads only.
     *
     * @param notificationUrl
     */
    public void setUploadDoneNotificationUrl(final String notificationUrl) {
        this.uploadDoneNotificationUrl = notificationUrl;
    }

    /**
     * The url to notify on the completion of uploading. This setting is used for multi-files uploads only.
     */
    public String getUploadDoneNotificationUrl() {
        return uploadDoneNotificationUrl;
    }

    /**
     * The maximal number of files to upload. This setting is used for multi-files uploads only.
     */
    public long getMaxNumberOfFiles() {
        return maxNumberOfFiles;
    }

    public String[] getAllowedExtensions(){
        return this.allowedExtensions;
    }

    /**
     * The setting used in {@link FileUploadBehavior} to determine when selecting a file, the widget should upload it
     * automatically or wait until the upload script is called.
     *
     * @see AbstractFileUploadWidget#getUploadScript()
     * @see org.hippoecm.frontend.plugins.jquery.upload.single.SingleFileUploadWidget#SingleFileUploadWidget(String, IPluginConfig, FileUploadValidationService, FileUploadPreProcessorService, boolean)
     */
    public boolean isAutoUpload() {
        return autoUpload;
    }

    /**
     * @see #isAutoUpload()
     */
    public final void setAutoUpload(final boolean autoUpload) {
        this.autoUpload = autoUpload;
    }

    /**
     * Sets the url to notify when the file selection list has been changed. This setting is used for
     * multi-file uploads only.
     * @param selectionChangeNotificationUrl
     */
    public void setSelectionChangeNotificationUrl(final String selectionChangeNotificationUrl) {
        this.selectionChangeNotificationUrl = selectionChangeNotificationUrl;
    }

    /**
     * @return the url to notify when the file selection list has been changed. This setting is used for multi-file
     * uploads only.
     */
    public String getSelectionChangeNotificationUrl() {
        return selectionChangeNotificationUrl;
    }
}
