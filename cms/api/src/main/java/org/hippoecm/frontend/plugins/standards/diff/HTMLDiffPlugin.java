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

package org.hippoecm.frontend.plugins.standards.diff;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * @author cngo
 * @version $Id$
 * @since 2015-02-02
 */
public class HTMLDiffPlugin extends Plugin {

    public static final String HTML_DIFFSERVICE_ID = "html.diffservice.id";

    /**
     * Construct plugin and register the {@link DefaultHtmlDiffService} to the context. The service id is retrieved from
     * plugin configuration parameter {@link DiffService#SERVICE_ID}
     *
     * @param context
     * @param config
     */
    public HTMLDiffPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        final String id = config.getString(DiffService.SERVICE_ID, HTML_DIFFSERVICE_ID);
        context.registerService(createDiffService(), id);
    }

    protected DiffService createDiffService() {
        return new DefaultHtmlDiffService(getPluginConfig());
    }
}
