/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;

public class FileUploadWidgetSettings implements IClusterable {
    public static final String MAX_WIDTH_PROP = "max.width";
    public static final String MAX_HEIGHT_PROP = "max.height";
    public static final String MAX_FILESIZE_PROP = "max.file.size";
    private static final long DEFAULT_MAX_NUMBER_OF_FILES = 25;

    private long maxWidth;
    private long maxHeight;
    private long maxFileSize;
    private String uploadUrl;
    private String uploadDoneNotificationUrl;
    private long maxNumberOfFiles;
    private String paramName;

    public FileUploadWidgetSettings(final IPluginConfig pluginConfig) {
        loadConfig(pluginConfig);
    }

    public void loadConfig(final IPluginConfig pluginConfig) {
        this.maxWidth = pluginConfig.getAsLong(MAX_WIDTH_PROP, ImageUploadValidationService.DEFAULT_MAX_WIDTH);
        this.maxHeight = pluginConfig.getAsLong(MAX_HEIGHT_PROP, ImageUploadValidationService.DEFAULT_MAX_HEIGHT);
        this.maxFileSize = Bytes.valueOf(pluginConfig.getString(MAX_FILESIZE_PROP, ImageUploadValidationService.DEFAULT_MAX_FILE_SIZE)).bytes();
        this.maxNumberOfFiles = pluginConfig.getAsLong("fileupload.maxItems", DEFAULT_MAX_NUMBER_OF_FILES);
    }

    public long getMaxWidth() {
        return maxWidth;
    }

    public long getMaxHeight() {
        return maxHeight;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String url) {
        this.uploadUrl = url;
    }

    public void setUploadDoneNotificationUrl(final String notificationUrl) {
        this.uploadDoneNotificationUrl = notificationUrl;
    }

    public String getUploadDoneNotificationUrl() {
        return uploadDoneNotificationUrl;
    }

    public long getMaxNumberOfFiles() {
        return maxNumberOfFiles;
    }

    public void setParamName(final String paramName) {
        this.paramName = paramName;
    }

    public String getParamName() {
        return paramName;
    }
}
