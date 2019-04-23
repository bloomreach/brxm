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
import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

class OpenUiBehaviorHeaderItem extends HeaderItem {

    private static final String OPEN_UI_BEHAVIOR_JS = "OpenUiBehavior.js";
    private static final JavaScriptResourceReference OPEN_UI_BEHAVIOR
            = new JavaScriptResourceReference(OpenUiBehaviorHeaderItem.class, OPEN_UI_BEHAVIOR_JS);

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton(OPEN_UI_BEHAVIOR_JS);
    }

    @Override
    public List<HeaderItem> getDependencies() {
        return Collections.singletonList(new PenpalHeaderItem());
    }

    @Override
    public void render(final Response response) {
        JavaScriptReferenceHeaderItem.forReference(OPEN_UI_BEHAVIOR).render(response);
    }
}
