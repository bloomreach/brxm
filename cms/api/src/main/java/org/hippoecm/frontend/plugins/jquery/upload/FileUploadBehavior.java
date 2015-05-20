/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;

/**
 * Contributes all CSS/JS resources needed by http://blueimp.github.com/jQuery-File-Upload/
 */
public abstract class FileUploadBehavior extends AbstractAjaxBehavior {
    private static final long serialVersionUID = 1L;

    private static final String AUTOUPLOAD_PARAM = "autoUpload";
    private static final String REGEX_ANYFILE = ".*";

    protected final FileUploadWidgetSettings settings;

    public FileUploadBehavior(final FileUploadWidgetSettings settings) {
        this.settings = settings;
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);

        renderCSS(response);
        renderScripts(response);

        renderWidgetConfig(response, configureParameters(component));
    }

    protected void renderCSS(final IHeaderResponse response) {
    }

    protected void renderScripts(final IHeaderResponse response) {
    }

    /**
     * Set parameters that will be used in the jquery-fileupload initialization. See jquery.fileupload.js/options
     */
    protected Map<String, Object> configureParameters(final Component component) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("componentMarkupId", component.getMarkupId());

        // the url to receive file upload
        variables.put("url", settings.getUploadUrl());

        variables.put("maxNumberOfFiles", settings.getMaxNumberOfFiles());

        // disable client-side file extension validation, accepted any files
        String acceptFileTypes = REGEX_ANYFILE;
        variables.put("acceptFileTypes", acceptFileTypes);

        // Get settings to configure the file upload widget
        variables.put(FileUploadWidgetSettings.MAX_WIDTH_PROP, settings.getMaxWidth());
        variables.put(FileUploadWidgetSettings.MAX_HEIGHT_PROP, settings.getMaxHeight());
        variables.put(FileUploadWidgetSettings.MAX_FILESIZE_PROP, settings.getMaxFileSize());
        variables.put(AUTOUPLOAD_PARAM, settings.isAutoUpload());
        return variables;
    }

    /**
     * Load the jquery-file-upload config script and run the startup method
     */
    abstract protected void renderWidgetConfig(final IHeaderResponse response, final Map<String, Object> variables);

    /**
     * Called when a request to a behavior is received.
     */
    @Override
    public void onRequest() {
    }
}
