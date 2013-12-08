/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest;

import java.util.List;

import javax.servlet.ServletContext;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class BaseResource {

    private static Logger log = LoggerFactory.getLogger(BaseResource.class);

    public List<Plugin> getPlugins(final ServletContext context) {
        final String libPath = context.getRealPath("/WEB-INF/lib");
        log.debug("Scanning path for essentials: {}", libPath);
        final PluginScanner scanner = new PluginScanner();
        return scanner.scan(libPath);
    }



    public Plugin getPluginByName(final String name, final ServletContext context) {
        final List<Plugin> plugins = getPlugins(context);
        for (final Plugin next : plugins) {
            if (next.getName().equals(name)) {
                return next;
            }
        }
        return null;
    }


}
