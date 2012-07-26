/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.addon.workflow;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.InheritingPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;

class PluginController implements IClusterable {

    private static final long serialVersionUID = 1L;

    private List<IClusterControl> clusters;
    private IPluginContext context;
    private IPluginConfig config;
    private String baseServiceName;
    
    PluginController(IPluginContext context, IPluginConfig config, String baseServiceName) {
        this.context = context;
        this.config = config;
        this.baseServiceName = baseServiceName;
        this.clusters = new LinkedList<IClusterControl>();
    }
    
    public void stopRenderers() {
        for (IClusterControl control : clusters) {
            control.stop();
        }
        clusters.clear();
    }
    
    public IRenderService startRenderer(IPluginConfig config) {
        if (config == null) {
            return null;
        }

        JavaClusterConfig childClusterConfig = new JavaClusterConfig();
        IPluginConfig childPluginConfig = new JavaPluginConfig(new InheritingPluginConfig(config, this.config));

        String serviceId = baseServiceName + "." + "id" + clusters.size();
        childPluginConfig.put(RenderService.WICKET_ID, serviceId);
        childClusterConfig.addPlugin(childPluginConfig);

        IClusterControl control = context.newCluster(childClusterConfig, null);
        control.start();

        clusters.add(control);

        return context.getService(serviceId, IRenderService.class);
    }

}
