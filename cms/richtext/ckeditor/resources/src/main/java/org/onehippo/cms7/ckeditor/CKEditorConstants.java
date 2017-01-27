/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.ckeditor;

import java.io.InputStream;

import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.util.io.IOUtils;
import org.hippoecm.frontend.util.WebApplicationHelper;

/**
 * Bundle for the CKEditor sources.
 */
public class CKEditorConstants {

    private CKEditorConstants() {
    }

    /**
     * Resource reference to the CKEditor source. To be able to load the CKEditor resources in a running container, a
     * {@code org.onehippo.cms7.utilities.servlet.ResourceServlet} should be configured that serves files from jars
     * that start with /ckeditor.
     */
    public static final UrlResourceReference CKEDITOR_OPTIMIZED_JS = WebApplicationHelper.createUniqueUrlResourceReference(Url.parse("ckeditor/optimized/ckeditor.js")).setContextRelative(true);
    public static final UrlResourceReference CKEDITOR_SRC_JS = WebApplicationHelper.createUniqueUrlResourceReference(Url.parse("ckeditor/ckeditor.js")).setContextRelative(true);

    /**
     * CKEDITOR constants for keyboard shortcuts
     */
    public static final int CTRL = 0x110000;
    public static final int SHIFT = 0x220000;
    public static final int ALT = 0x440000;

    /**
     * The CKEDITOR.config properties
     */
    public static final String CONFIG_CONTENTS_CSS = "contentsCss";
    public static final String CONFIG_CUSTOM_CONFIG = "customConfig";
    public static final String CONFIG_EXTRA_PLUGINS = "extraPlugins";
    public static final String CONFIG_KEYSTROKES = "keystrokes";
    public static final String CONFIG_LANGUAGE = "language";
    public static final String CONFIG_STYLES_SET = "stylesSet";

    /**
     * CKEditor plugin names
     */
    public static final String PLUGIN_DIVAREA = "divarea";

    /**
     * Checks whether a CKEditor resource reference exists on the class path.
     * @param ref a CKEditor resource reference.
     */
    public static boolean existsOnClassPath(final UrlResourceReference ref) {
        final String path = ref.getUrl().getPath();
        final InputStream resource = CKEditorConstants.class.getResourceAsStream("/" + path);
        IOUtils.closeQuietly(resource);
        return resource != null;
    }

}
