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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;

/**
 * Contributes all CSS/JS resources needed by http://blueimp.github.com/jQuery-File-Upload/
 */
public class FileUploadBehavior extends AbstractAjaxBehavior {
    private static final long serialVersionUID = 1L;

    public static final String[] JQUERY_FILEUPLOAD_CSS={
            "css/jquery.fileupload-ui.css"
    };

    public static final String[] JQUERY_FILEUPLOAD_SCRIPTS={
            "js/jquery.ui.widget.js",
            "js/tmpl.js",
            "js/load-image.js",
            "js/load-image-meta.js",
            "js/canvas-to-blob.js",
            "js/jquery.iframe-transport.js",
            "js/jquery.fileupload.js",
            "js/jquery.fileupload-process.js",
            "js/jquery.fileupload-image.js",
            "js/jquery.fileupload-validate.js",
            "js/jquery.fileupload-ui.js"
    };
    public static final String CONFIG_JS = "js/main.js";

    public static final String STARTUP_SCRIPT = "jqueryFileUploadImpl.init();";

    private final FileUploadWidgetSettings settings;

    public FileUploadBehavior(final FileUploadWidgetSettings settings) {
        this.settings = settings;
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);

        for(String css : JQUERY_FILEUPLOAD_CSS){
            response.render(CssHeaderItem.forReference(
                    new CssResourceReference(FileUploadBehavior.class, css)));
        }

        response.render(JavaScriptHeaderItem.forReference(
                component.getApplication().getJavaScriptLibrarySettings().getJQueryReference()));

        for(String js : JQUERY_FILEUPLOAD_SCRIPTS){
            response.render(JavaScriptHeaderItem.forReference(
                    new JavaScriptResourceReference(FileUploadBehavior.class, js)));
        }

        configureWidget(component, response);
    }

    private void configureWidget(final Component component, final IHeaderResponse response) {
        PackageTextTemplate jsTmpl = new PackageTextTemplate(FileUploadBehavior.class, CONFIG_JS);
        Map<String, Object> variables = new HashMap<>();

        variables.put("componentMarkupId", component.getMarkupId());

        // the url to receive file upload
        variables.put("url", settings.getUploadUrl());

        // the name of file input field
        variables.put("paramName", settings.getParamName());

        variables.put("maxNumberOfFiles", settings.getMaxNumberOfFiles());

        // accepted image file extensions
        final String acceptFileTypes = StringUtils.join(settings.getAllowedExtensions(), "|");
        variables.put("acceptFileTypes", acceptFileTypes);

        //the url to be notified when uploading has done
        variables.put("fileUploadDoneUrl", settings.getUploadDoneNotificationUrl());

        // Get settings to configure the file upload widget
        variables.put(FileUploadWidgetSettings.MAX_WIDTH_PROP, settings.getMaxWidth());
        variables.put(FileUploadWidgetSettings.MAX_HEIGHT_PROP, settings.getMaxHeight());
        variables.put(FileUploadWidgetSettings.MAX_FILESIZE_PROP, settings.getMaxFileSize());

        String s = jsTmpl.asString(variables);
        response.render(JavaScriptHeaderItem.forScript(s, "fileupload"));

        // call the configuration after all DOM elements are loaded
        response.render(OnDomReadyHeaderItem.forScript(STARTUP_SCRIPT));
    }

    /**
     * Called when a request to a behavior is received.
     */
    @Override
    public void onRequest() {

    }
}
