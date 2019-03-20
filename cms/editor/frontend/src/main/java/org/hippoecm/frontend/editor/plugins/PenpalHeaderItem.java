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

package org.hippoecm.frontend.editor.plugins;

import java.util.Collections;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

class PenpalHeaderItem extends HeaderItem {

    // penpal.js is fetched by npm
    private static final String PENPAL_JS = "penpal.js";
    private static final ResourceReference PENPAL = new JavaScriptResourceReference(PenpalHeaderItem.class, PENPAL_JS);

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton(PENPAL_JS);
    }

    @Override
    public void render(final Response response) {
        JavaScriptHeaderItem.forReference(PENPAL).render(response);
    }
}
