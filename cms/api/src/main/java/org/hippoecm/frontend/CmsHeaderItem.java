/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import java.util.Collections;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

public class CmsHeaderItem extends HippoHeaderItem {

    private static final ResourceReference MESSAGE_BUS_JS = new JavaScriptResourceReference(HippoHeaderItem.class, "js/message-bus.js");

    private static final CmsHeaderItem INSTANCE = new CmsHeaderItem();

    public static CmsHeaderItem get() {
        return INSTANCE;
    }

    private CmsHeaderItem() {
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("hippo-cms-header-item");
    }

    @Override
    public void render(final Response response) {
        CssHeaderItem.forReference(SCREEN_CSS).render(response);

        if (isBrowserInternetExplorer()) {
            CssHeaderItem.forReference(SCREEN_IE_CSS).render(response);
        }

        CssHeaderItem.forReference(isDevelopmentMode() ? THEME_MIN_CSS : THEME_CSS).render(response);

        JavaScriptReferenceHeaderItem.forReference(GLOBAL_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(FUTURE_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(MESSAGE_BUS_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(isDevelopmentMode() ? THEME_MIN_JS : THEME_JS).render(response);

        if (isBrowserInternetExplorer() && getBrowserVersion() == 11) {
            JavaScriptReferenceHeaderItem.forReference(IE11_JS).render(response);
        }
    }
}
