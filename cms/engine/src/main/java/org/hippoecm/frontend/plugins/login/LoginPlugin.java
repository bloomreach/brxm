/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.mapper.AbstractComponentMapper;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.info.PageComponentInfo;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.PluginApplication;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.settings.GlobalSettings;
import org.hippoecm.frontend.usagestatistics.UsageStatisticsSettings;

public class LoginPlugin extends RenderPlugin {

    public static final String TERMS_AND_CONDITIONS_LINK = "https://www.bloomreach.com/en/about/privacy";

    private static final ResourceReference DEFAULT_FAVICON = new UrlResourceReference(
            Url.parse("skin/images/hippo-cms.ico"));

    private static final String EDITION = "edition";
    private static final String AUTOCOMPLETE = "signin.form.autocomplete";
    private static final String LOCALES = "locales";
    private static final String SUPPORTED_BROWSERS = "browsers.supported";
    
    public static final String[] DEFAULT_LOCALES = {"en"};

    private ResourceReference editionCss;
    private PageParameters parameters;

    public LoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(CssClass.append("hippo-login-plugin"));

        add(new Label("pageTitle", getString("page.title")));

        final ResourceReference iconReference = getFaviconReference();
        add(new ResourceLink("faviconLink", iconReference));

        String[] supported = config.getStringArray(SUPPORTED_BROWSERS);
        if (supported != null) {
            add(new BrowserCheckBehavior(supported));
        }

        // In case of using a different edition, add extra CSS rules to show the required styling
        if (config.containsKey(EDITION)) {
            final String edition = config.getString(EDITION);
            editionCss = new CssResourceReference(LoginPlugin.class, "login_" + edition + ".css");
        }

        ExternalLink termsAndConditions = new ExternalLink("termsAndConditions", TERMS_AND_CONDITIONS_LINK) {
            @Override
            public boolean isVisible() {
                return UsageStatisticsSettings.get().isEnabled();
            }
        };
        termsAndConditions.setOutputMarkupId(true);
        add(termsAndConditions);

        final boolean autoComplete = getPluginConfig().getAsBoolean(AUTOCOMPLETE, true);
        String[] localeArray = GlobalSettings.get().getStringArray(LOCALES);
        if (localeArray == null || localeArray.length == 0) {
            localeArray = DEFAULT_LOCALES;
        }
        add(createLoginPanel("login-panel", autoComplete, Arrays.asList(localeArray), new LoginPluginHandler(termsAndConditions)));

        add(new Label("pinger"));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(LoginHeaderItem.get());
        if (editionCss != null) {
            response.render(CssHeaderItem.forReference(editionCss));
        }
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        Request request = RequestCycle.get().getRequest();

        /**
         * Strip the first query parameter from URL if it is empty
         * Copied from {@link AbstractComponentMapper#removeMetaParameter}
         */
        Url urlCopy = new Url(request.getUrl());
        if (!urlCopy.getQueryParameters().isEmpty() &&
                Strings.isEmpty(urlCopy.getQueryParameters().get(0).getValue())) {
            String pageComponentInfoCandidate = urlCopy.getQueryParameters().get(0).getName();
            if (PageComponentInfo.parse(pageComponentInfoCandidate) != null) {
                urlCopy.getQueryParameters().remove(0);
            }
        }

        parameters = new PageParametersEncoder().decodePageParameters(urlCopy);
    }

    protected ResourceReference getFaviconReference() {
        final PluginApplication application = (PluginApplication) getApplication();
        final ResourceReference reference = application.getPluginApplicationFavIconReference();
        return reference != null ? reference: DEFAULT_FAVICON;
    }

    protected LoginPanel createLoginPanel(final String id, final boolean autoComplete, final List<String> locales,
                                          final LoginHandler handler) {
        return new LoginPanel(id, autoComplete, locales, handler);
    }

    private class LoginPluginHandler implements LoginHandler {

        private final ExternalLink privacyTerms;

        public LoginPluginHandler(final ExternalLink privacyTerms) {
            this.privacyTerms = privacyTerms;
        }

        @Override
        public void localeChanged(final String selectedLocale, final AjaxRequestTarget target) {
            // show a localized privacy & terms link when the selected locale changes
            target.add(privacyTerms);
        }

        @Override
        public void loginSuccess() {
            if (parameters != null) {
                setResponsePage(PluginPage.class, parameters);
            } else {
                setResponsePage(PluginPage.class);
            }
        }
    }
}
