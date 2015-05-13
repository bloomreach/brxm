/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings;

public class GalleryFileUploadBehavior extends FileUploadBehavior {
    public static final String CONFIG_JS = "fileupload-gallery-config.js";

    public static final String[] JQUERY_FILEUPLOAD_UI_EXT_SCRIPTS = {
            "js/tmpl.js",
            "js/load-image.js",
            "js/load-image-meta.js",
            "js/canvas-to-blob.js",
            "js/jquery.fileupload-image.js",
            "js/jquery.fileupload-ui.js",
            "multiple/jquery.fileupload-ui-gallery.js"
    };

    private static final String[] JQUERY_FILEUPLOAD_UI_CSS = {
            "css/jquery.fileupload-ui.css",
            "css/jquery.fileupload-ui-hippo.css"
    };

    public GalleryFileUploadBehavior(final FileUploadWidgetSettings settings) {
        super(settings);
    }

    protected void renderCSS(final IHeaderResponse response) {
        super.renderCSS(response);
        for(String css : JQUERY_FILEUPLOAD_UI_CSS){
            response.render(CssHeaderItem.forReference(
                    new CssResourceReference(FileUploadBehavior.class, css)));
        }
    }

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
        return variables;
    }

    @Override
    protected void renderWidgetConfig(final IHeaderResponse response, final Map<String, Object> variables) {
        PackageTextTemplate jsTmpl = new PackageTextTemplate(GalleryFileUploadBehavior.class, CONFIG_JS);
        String s = jsTmpl.asString(variables);
        response.render(OnDomReadyHeaderItem.forScript(s));
    }
}
