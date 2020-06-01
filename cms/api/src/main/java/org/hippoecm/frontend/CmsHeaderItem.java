/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.TimeZone;

import javax.servlet.http.HttpSession;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

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
        CssHeaderItem.forReference(isDevelopmentMode() ? THEME_CSS : THEME_MIN_CSS).render(response);

        JavaScriptReferenceHeaderItem.forReference(GLOBAL_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(FUTURE_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(MESSAGE_BUS_JS).render(response);
        JavaScriptReferenceHeaderItem.forReference(isDevelopmentMode() ? THEME_JS : THEME_MIN_JS).render(response);

        publishAntiCacheHash(response);
        publishSessionParameters(response);
    }

    private void publishAntiCacheHash(final Response response) {
        final String script = "Hippo.antiCache = '" + WebApplicationHelper.APPLICATION_HASH + "';";
        JavaScriptHeaderItem.forScript(script, "hippo-anti-cache-hash").render(response);
    }

    private void publishSessionParameters(final Response response) {
        final ServletWebRequest servletWebRequest = (ServletWebRequest) RequestCycle.get().getRequest();
        final HttpSession httpSession = servletWebRequest.getContainerRequest().getSession();
        final CmsSessionContext sessionContext = CmsSessionContext.getContext(httpSession);
        if (sessionContext != null) {
            final StringBuilder script = new StringBuilder();

            final String locale = sessionContext.getLocale().getLanguage();
            final TimeZone timezone = UserSession.get().getClientInfo().getProperties().getTimeZone();

            script.append("Hippo.Session = {};");
            script.append("Hippo.Session.locale = '");
            script.append(locale);
            script.append("';");

            if (timezone != null) {
                script.append("Hippo.Session.timezone = '");
                script.append(timezone.getID());
                script.append("';");
            }

            JavaScriptHeaderItem.forScript(script, "hippo-session-parameters").render(response);
        }
    }

}
