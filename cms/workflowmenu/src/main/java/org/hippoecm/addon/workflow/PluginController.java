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
package org.hippoecm.addon.workflow;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.InheritingPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;

class PluginController implements IClusterable {

    private static final long serialVersionUID = 1L;

    private static class Cluster implements Serializable {
        private final IClusterControl control;
        private final ModelReference<?> modelService;

        private Cluster(final IClusterControl control, final ModelReference<?> modelService) {
            this.control = control;
            this.modelService = modelService;
        }

        void stop() {
            control.stop();
            modelService.destroy();
        }
    }

    private List<Cluster> clusters;
    private IPluginContext context;
    private IPluginConfig config;
    private String baseServiceName;
    
    PluginController(IPluginContext context, IPluginConfig config, String baseServiceName) {
        this.context = context;
        this.config = config;
        this.baseServiceName = baseServiceName;
        this.clusters = new LinkedList<Cluster>();
    }
    
    public void stopRenderers() {
        for (Cluster control : clusters) {
            control.stop();
        }
        clusters.clear();
    }
    
    public IRenderService startRenderer(IPluginConfig config, WorkflowDescriptorModel wdm) {
        if (config == null) {
            return null;
        }

        String wicketModelId = baseServiceName + "." + "model" + clusters.size();
        ModelReference modelRef = new ModelReference(wicketModelId, wdm);
        modelRef.init(context);

        JavaClusterConfig childClusterConfig = new JavaClusterConfig();
        IPluginConfig childPluginConfig = new JavaPluginConfig(new InheritingPluginConfig(config, this.config));

        String wicketRenderId = baseServiceName + "." + "id" + clusters.size();
        childPluginConfig.put(RenderService.WICKET_ID, wicketRenderId);
        childPluginConfig.put(RenderService.MODEL_ID, wicketModelId);
        childClusterConfig.addPlugin(childPluginConfig);

        IClusterControl control = context.newCluster(childClusterConfig, null);
        control.start();

        clusters.add(new Cluster(control, modelRef));

        return context.getService(wicketRenderId, IRenderService.class);
    }

    public IRenderService startRenderer(IClusterConfig config, WorkflowDescriptorModel wdm) {
        if (config == null) {
            return null;
        }

        String wicketModelId = baseServiceName + "." + "model" + clusters.size();
        ModelReference modelRef = new ModelReference(wicketModelId, wdm);
        modelRef.init(context);

        JavaPluginConfig parameters = new JavaPluginConfig(new InheritingPluginConfig(config, this.config));

        String wicketRenderId = baseServiceName + "." + "id" + clusters.size();
        parameters.put(RenderService.WICKET_ID, wicketRenderId);
        parameters.put(RenderService.MODEL_ID, wicketModelId);

        IClusterControl control = context.newCluster(config, parameters);
        control.start();

        clusters.add(new Cluster(control, modelRef));

        return context.getService(wicketRenderId, IRenderService.class);
    }

}
