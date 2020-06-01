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
package org.onehippo.cms7.reports;

import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.wicketstuff.js.ext.ExtComponent;

/**
 * The root class for the plugins which want to be treated as ExtJS based plugins.
 * The ExtJs based plugins provide the {@link ExtComponent} rather than a plain wicket component,
 * which in turn be added to any ExtPlugin aware root component, e.g. {@link com.onehippo.cms7.reports.layout.ReportsRenderPlugin}
 */
public interface ExtPlugin extends IPlugin {

    /**
     * Should return the ExtPanel that needs to be added to the parent plugin, that manages this plugin.
     *
     * @return ExtComponent - The root ExtComponent that is provided by this plugin.
     */
    public ExtComponent getExtComponent();

}
