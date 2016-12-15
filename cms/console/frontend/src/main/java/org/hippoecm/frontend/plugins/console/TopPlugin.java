/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(TopPlugin.class);

    private static final String BAR_STYLES = "bar.styles";
    private static final String BAR_STYLE_URLPARTS = "bar.style.urlparts";
    private static final SystemInfoDataProvider SYSTEM_INFO = new SystemInfoDataProvider();

    public TopPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final WebMarkupContainer logo = new WebMarkupContainer("logo");
        logo.add(TitleAttribute.set("Hippo Release Version: " + SYSTEM_INFO.getReleaseVersion()));
        add(logo);

        final WebComponent toolbar = new WebComponent(BAR_STYLES);
        final String style = obtainBreadcrumbStyle(config);
        if (StringUtils.isNotBlank(style)) {
            toolbar.add(AttributeModifier.replace("style", style));
        }
        add(toolbar);
    }

    public String obtainBreadcrumbStyle(final IPluginConfig config) {
        final String[] urlParts = config.getStringArray(BAR_STYLE_URLPARTS);
        final String[] barStyles = config.getStringArray(BAR_STYLES);

        if (urlParts == null || barStyles == null) {
            return null;
        }
        if (urlParts.length != barStyles.length) {
            log.warn("Number of values on the properties \"{}\" and \"{}\" must be equal on {}",
                    BAR_STYLES, BAR_STYLE_URLPARTS, config);
            return null;
        }

        final String requestUrl = getRequestUrl();
        if (requestUrl != null) {
            for (int i = 0; i < urlParts.length; i++) {
                final String urlPart = urlParts[i];
                final String barStyle = barStyles[i];
                if (StringUtils.isNotEmpty(urlPart) && requestUrl.contains(urlPart) && StringUtils.isNotBlank(barStyle)) {
                    return barStyle;
                }
            }
        }

        return null;
    }

    private String getRequestUrl() {
        final Object request = getRequest().getContainerRequest();
        if (request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getRequestURL().toString();
        }
        return null;
    }
}
