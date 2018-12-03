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
package org.hippoecm.frontend.plugins.login;

import java.util.Collections;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.HippoHeaderItem;

public class LoginHeaderItem extends HippoHeaderItem {

    private static final LoginHeaderItem INSTANCE = new LoginHeaderItem();

    protected static final ResourceReference THEME_CSS = getUniqueResourceReference("skin/hippo-cms/css/br-login-theme.css");
    protected static final ResourceReference THEME_MIN_CSS = getUniqueResourceReference("skin/hippo-cms/css/br-login-theme.min.css");
    protected static final ResourceReference INIT_JS = new JavaScriptResourceReference(LoginHeaderItem.class, "login-init.js");

    public static LoginHeaderItem get() {
        return INSTANCE;
    }

    private LoginHeaderItem() {
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singletonList("login-header-item");
    }

    @Override
    public void render(final Response response) {
        CssHeaderItem.forReference(isDevelopmentMode() ? THEME_MIN_CSS : THEME_CSS).render(response);
        JavaScriptReferenceHeaderItem.forReference(GLOBAL_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(INIT_JS).render(response);
    }
}
