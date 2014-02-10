/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(TopPlugin.class);

    public TopPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        WebComponent toolbar = new WebComponent("bar.styles");

        String style = obtainBreadcrumbStyle(config);
        if(StringUtils.isNotBlank(style)) {
            toolbar.add(new AttributeModifier("style", style));
        }

        add(toolbar);
    }

    public String obtainBreadcrumbStyle(IPluginConfig config){
        String[] urlParts = config.getStringArray("bar.style.urlparts");
        String[] barStyles = config.getStringArray("bar.styles");

        if(urlParts == null || barStyles == null) {
            return null;
        }
        if(urlParts.length != barStyles.length) {
            log.warn("Number of values on the properties \"bar.style.urlparts\" and \"bar.styles\" must be equal on "
                    + config);
            return null;
        }

        String requestUrl = getRequestUrl();
        if(requestUrl != null) {
            for (int i = 0 ; i < urlParts.length; i++) {
                if (StringUtils.isNotEmpty(urlParts[i])) {
                    String urlPart = urlParts[i];
                    if(requestUrl.contains(urlPart) && StringUtils.isNotBlank(barStyles[i])) {
                        return barStyles[i];
                    }
                }
            }
        }

        return null;
    }

    private String getRequestUrl() {
        final Object request = getRequest().getContainerRequest();
        if(request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getRequestURL().toString();
        }
        return null;
    }
}
