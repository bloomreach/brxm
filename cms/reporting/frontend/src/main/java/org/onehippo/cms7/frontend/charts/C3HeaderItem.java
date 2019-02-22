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

package org.onehippo.cms7.frontend.charts;

import java.util.Collections;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class C3HeaderItem extends HeaderItem {

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("c3js");
    }

    @Override
    public void render(final Response response) {
        renderCss("c3.css", response);
        renderJavaScript("d3.js", response);
        renderJavaScript("c3.js", response);
    }

    private void renderCss(final String relPath, final Response response) {
        final CssResourceReference css = new CssResourceReference(C3HeaderItem.class, relPath);
        CssHeaderItem.forReference(css).render(response);
    }

    private void renderJavaScript(final String relPath, final Response response) {
        final JavaScriptResourceReference js = new JavaScriptResourceReference(C3HeaderItem.class, relPath);
        JavaScriptHeaderItem.forReference(js).render(response);
    }

}
