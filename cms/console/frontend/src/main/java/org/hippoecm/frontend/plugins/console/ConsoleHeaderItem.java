/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.console;

import java.util.Collections;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.HippoHeaderItem;

public class ConsoleHeaderItem extends HippoHeaderItem {

    protected static final ResourceReference CONSOLE_CSS = new CssResourceReference(ConsoleHeaderItem.class, "console.css");
    protected static final ResourceReference TREE_CSS = new CssResourceReference(ConsoleHeaderItem.class, "tree/tree.css");
    protected static final ResourceReference FA_CSS = new CssResourceReference(RootPlugin.class, "fontawesome/css/font-awesome.css");

    private static final ConsoleHeaderItem INSTANCE = new ConsoleHeaderItem();

    public static ConsoleHeaderItem get() {
        return INSTANCE;
    }

    private ConsoleHeaderItem() {
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singletonList("console-header-item");
    }

    @Override
    public void render(final Response response) {
        CssHeaderItem.forReference(LEGACY_CSS).render(response);
        CssHeaderItem.forReference(CONSOLE_CSS).render(response);
        CssHeaderItem.forReference(TREE_CSS).render(response);
        CssHeaderItem.forReference(FA_CSS).render(response);

        JavaScriptReferenceHeaderItem.forReference(FUTURE_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(GLOBAL_JS).render(response);
    }
}
