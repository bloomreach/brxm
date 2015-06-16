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
package org.onehippo.hst.demo.qa;

import javax.jcr.Node;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

/**
 * Adds a client-side log for usage statistics events to the CMS. The log mocks the normal integration with
 * segment.com, and allows QA essentials to verify the generated events.
 */
public class UsageStatisticsLoggerPlugin extends RenderPlugin<Node> {

    public static final JavaScriptResourceReference USAGE_STATISTICS_LOGGER_JS =
            new JavaScriptResourceReference(UsageStatisticsLoggerPlugin.class, "usage-statistics-logger.js");

    public UsageStatisticsLoggerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(USAGE_STATISTICS_LOGGER_JS));
    }

}
