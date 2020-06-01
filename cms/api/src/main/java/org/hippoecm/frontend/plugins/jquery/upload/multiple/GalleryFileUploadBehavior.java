/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload.multiple;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;

public class GalleryFileUploadBehavior extends FileUploadBehavior {
    public static final String CONFIG_JS = "fileupload-gallery-config.js";

    public static final String[] JQUERY_FILEUPLOAD_UI_EXT_SCRIPTS = {
            // the jquery.fileupload-ui.js file is fetched by npm
            "multiple/jquery.fileupload-ui.js",
            "multiple/jquery.fileupload-ui-gallery.js"
    };

    private static final String MAX_FILESIZE_MESSAGE = "max.filesize.message";
    private static final String INVALID_EXTENSION_MESSAGE = "invalid.extension.message";
    private static final String MAX_NUMBER_OF_FILES_EXCEEDED = "max.number.of.files.exceeded";
    private static final String MAX_NUMBER_OF_FILES_EXCEEDED_WIDGET = "max.number.of.files.exceeded.widget";

    public GalleryFileUploadBehavior(final FileUploadWidgetSettings settings) {
        super(settings);
    }

    @Override
    protected void renderScripts(final IHeaderResponse response) {
        super.renderScripts(response);
        // insert other scripts for multi-file upload purposes
        for(String js : JQUERY_FILEUPLOAD_UI_EXT_SCRIPTS) {
            response.render(JavaScriptHeaderItem.forReference(
                    new JavaScriptResourceReference(FileUploadBehavior.class, js)));
        }
    }

    @Override
    protected Map<String, Object> configureParameters(final Component component) {
        final Map<String, Object> variables = super.configureParameters(component);
        //the url to be notified when uploading has done
        variables.put("fileUploadDoneUrl", settings.getUploadDoneNotificationUrl());
        variables.put("selectionChangeUrl", settings.getSelectionChangeNotificationUrl());

        final List<String> allowedExtensions = Arrays.asList(ArrayUtils.nullToEmpty(settings.getAllowedExtensions()));

        variables.put("acceptFileTypes", getAcceptFileTypesPattern("|"));

        // Localized error messages
        variables.put(MAX_NUMBER_OF_FILES_EXCEEDED_WIDGET, getMessage(MAX_NUMBER_OF_FILES_EXCEEDED_WIDGET));

        final String maxFileSizeMB = String.format("%2.1fMB", Bytes.bytes(settings.getMaxFileSize()).megabytes());
        variables.put(MAX_FILESIZE_MESSAGE, getMessage(MAX_FILESIZE_MESSAGE, maxFileSizeMB));

        variables.put(INVALID_EXTENSION_MESSAGE, getMessage(INVALID_EXTENSION_MESSAGE, getAcceptFileTypesPattern(", ")));
        return variables;
    }

    private String getAcceptFileTypesPattern(String delimiter) {
        final String[] allowedExtensions = settings.getAllowedExtensions();
        if (allowedExtensions == null || allowedExtensions.length == 0) {
            return "*";
        }

        return "(" + String.join(delimiter, allowedExtensions) + ")";
    }

    private String getMessage(final String key, Object... parameters) {
        return new ClassResourceModel(key, GalleryFileUploadBehavior.class, parameters).getObject();
    }

    @Override
    protected void renderWidgetConfig(final IHeaderResponse response, final Map<String, Object> variables) {
        PackageTextTemplate jsTmpl = new PackageTextTemplate(GalleryFileUploadBehavior.class, CONFIG_JS);
        String s = jsTmpl.asString(variables);
        response.render(OnDomReadyHeaderItem.forScript(s));
    }
}
