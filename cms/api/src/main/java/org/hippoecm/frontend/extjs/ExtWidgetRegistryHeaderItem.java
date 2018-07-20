/*
 * Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.extjs;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class ExtWidgetRegistryHeaderItem extends HeaderItem {

    private static final JavaScriptResourceReference EXT_WIDGET_REGISTRY_JS = new JavaScriptResourceReference(ExtWidgetRegistry.class, "ExtWidgetRegistry.js");

    private static final ExtWidgetRegistryHeaderItem INSTANCE = new ExtWidgetRegistryHeaderItem();

    public static ExtWidgetRegistryHeaderItem get() {
        return INSTANCE;
    }

    private ExtWidgetRegistryHeaderItem() {}

    @Override
    public List<HeaderItem> getDependencies() {
        return Arrays.asList(ExtResourcesHeaderItem.get());
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Arrays.asList("ext-widget-registry-header-item");
    }

    @Override
    public void render(final Response response) {
        JavaScriptHeaderItem.forReference(EXT_WIDGET_REGISTRY_JS).render(response);
    }
}
