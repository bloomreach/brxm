/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.reports.plugins.brokenlinkslist;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.reports.AbstractExtRenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.ExtPanel;

public class BrokenLinksListPlugin extends AbstractExtRenderPlugin {

    private static final String PROP_QUERY = "query";

    private Logger log = LoggerFactory.getLogger(BrokenLinksListPlugin.class);

    private ExtPanel panel;

    public BrokenLinksListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String query = config.getString(PROP_QUERY);
        if (query == null) {
            log.error("Report configuration '{}' is missing the required string property '{}' ",
                    config.getName(), PROP_QUERY);
            return;
        }

        panel = new BrokenLinksListPanel(context, config, query);
        add(panel);
    }

    public ExtComponent getExtComponent() {
        return panel;
    }

}
