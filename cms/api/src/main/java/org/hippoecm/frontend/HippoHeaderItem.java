/*
 *  Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.hippoecm.frontend.util.WebApplicationHelper;

public abstract class HippoHeaderItem extends HeaderItem {

    protected static final ResourceReference SCREEN_CSS = getUniqueResourceReference("skin/screen.css");
    protected static final ResourceReference SCREEN_IE_CSS = getUniqueResourceReference("skin/screen_ie.css");
    protected static final ResourceReference LEGACY_CSS = getUniqueResourceReference("skin/screen_legacy.css");

    protected static final ResourceReference THEME_CSS = getUniqueResourceReference("skin/hippo-cms/css/hippo-cms-theme.css");
    protected static final ResourceReference THEME_MIN_CSS = getUniqueResourceReference("skin/hippo-cms/css/hippo-cms-theme.min.css");

    protected static final ResourceReference THEME_JS = getUniqueResourceReference("skin/hippo-cms/js/hippo-cms-theme.js");
    protected static final ResourceReference THEME_MIN_JS = getUniqueResourceReference("skin/hippo-cms/js/hippo-cms-theme.min.js");

    protected static final ResourceReference FUTURE_JS = new JavaScriptResourceReference(HippoHeaderItem.class, "js/future.js");
    protected static final ResourceReference GLOBAL_JS = new JavaScriptResourceReference(HippoHeaderItem.class, "js/global.js");
    protected static final ResourceReference IE11_JS = new JavaScriptResourceReference(HippoHeaderItem.class, "js/ie11.js");

    protected static UrlResourceReference getUniqueResourceReference(final String path) {
        return WebApplicationHelper.createUniqueUrlResourceReference(Url.parse(path)).setContextRelative(true);
    }

    protected boolean isBrowserInternetExplorer() {
        return WebSession.get().getClientInfo().getProperties().isBrowserInternetExplorer();
    }

    protected int getBrowserVersion() {
        return WebSession.get().getClientInfo().getProperties().getBrowserVersionMajor();
    }

    protected boolean isDevelopmentMode() {
        return Application.get().usesDeploymentConfig();
    }
}
