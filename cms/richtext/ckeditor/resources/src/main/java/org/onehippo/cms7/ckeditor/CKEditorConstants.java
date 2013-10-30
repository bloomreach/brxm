/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;

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
    public static final ResourceReference CKEDITOR_JS = new UrlResourceReference(new Url(Arrays.asList("ckeditor", "ckeditor.js"), Charset.forName("UTF-8")));

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
    public static final String CONFIG_LANGUAGE = "language";
    public static final String CONFIG_STYLES_SET = "stylesSet";

    /**
     * CKEditor plugin names
     */
    public static final String PLUGIN_DIVAREA = "divarea";

}
