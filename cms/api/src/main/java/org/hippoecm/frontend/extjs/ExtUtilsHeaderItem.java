/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.extjs;

import java.util.Arrays;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class ExtUtilsHeaderItem extends HeaderItem {

    private static final JavaScriptResourceReference EXT_MESSAGE_BUS = new JavaScriptResourceReference(ExtWidgetRegistry.class, "MessageBus.js");
    private static final JavaScriptResourceReference EXT_IFRAME_PANEL = new JavaScriptResourceReference(ExtWidgetRegistry.class, "IFramePanel.js");

    private static final ExtUtilsHeaderItem INSTANCE = new ExtUtilsHeaderItem();

    public static ExtUtilsHeaderItem get() {
        return INSTANCE;
    }

    private ExtUtilsHeaderItem() {}

    @Override
    public Iterable<? extends HeaderItem> getDependencies() {
        return Arrays.asList(ExtResourcesHeaderItem.get());
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Arrays.asList("ext-utils-header-item");
    }

    @Override
    public void render(final Response response) {
        JavaScriptHeaderItem.forReference(EXT_MESSAGE_BUS).render(response);
        JavaScriptHeaderItem.forReference(EXT_IFRAME_PANEL).render(response);
    }
}
