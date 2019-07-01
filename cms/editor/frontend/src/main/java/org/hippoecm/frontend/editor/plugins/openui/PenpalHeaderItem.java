/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.editor.plugins.openui;

import java.util.Collections;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.util.WebApplicationHelper;

class PenpalHeaderItem extends HeaderItem {

    // penpal.js is fetched by npm
    private static final String PENPAL_JS = "penpal.js";
    private static final ResourceReference PENPAL = new JavaScriptResourceReference(PenpalHeaderItem.class, PENPAL_JS);

    private static final String PENPAL_MIN_JS = "penpal.min.js";
    private static final ResourceReference PENPAL_MINIFIED = new JavaScriptResourceReference(PenpalHeaderItem.class, PENPAL_MIN_JS);

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton(PENPAL_JS);
    }

    @Override
    public void render(final Response response) {
        if (WebApplicationHelper.isDevelopmentMode()) {
            JavaScriptHeaderItem.forReference(PENPAL).render(response);
        } else {
            // Use the minified version for performance (smaller file) and to prevent Wicket's internal JavaScript
            // compressor from breaking the file. Wicket's default application settings don't use a JavaScript
            // compressor in "development" mode and a DefaultJavaScriptCompressor in "production" mode. The
            // DefaultJavaScriptProcessor breaks the Penpal code. But: it won't minify JavaScript when its name
            // contains ".min.". So by using penpal.min.js in production everything works fine.
            JavaScriptHeaderItem.forReference(PENPAL_MINIFIED).render(response);
        }
    }
}
