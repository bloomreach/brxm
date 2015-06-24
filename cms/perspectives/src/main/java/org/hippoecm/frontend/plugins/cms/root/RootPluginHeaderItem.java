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
package org.hippoecm.frontend.plugins.cms.root;

import java.util.Collections;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.HippoHeaderItem;

public class RootPluginHeaderItem extends HippoHeaderItem {

    private static final String REMOVE_EXT_BROWSER_CLASSES_JS = "remove-ext-browser-classes.js";

    private static final RootPluginHeaderItem INSTANCE = new RootPluginHeaderItem();

    public static RootPluginHeaderItem get() {
        return INSTANCE;
    }

    private RootPluginHeaderItem() {
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("root-plugin-header-item");
    }

    @Override
    public void render(final Response response) {
        createRemoveExtBrowserClassesScript().render(response);
    }

    private HeaderItem createRemoveExtBrowserClassesScript() {
        final PackageTextTemplate removeScriptTemplate = new PackageTextTemplate(RootPluginHeaderItem.class, REMOVE_EXT_BROWSER_CLASSES_JS);
        final String javaScript = removeScriptTemplate.asString();
        return OnLoadHeaderItem.forScript(javaScript);
    }

}
