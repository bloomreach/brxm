/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class TestWorkflowManagerPlugin extends AbstractWorkflowManagerPlugin {

    public TestWorkflowManagerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void render(final PluginRequestTarget target) {
        Set<Node> nodeSet = new HashSet<>();
        nodeSet.add(getModelObject());
        MenuHierarchy menu = buildMenu(nodeSet);
        menu.restructure();
        replace(new MenuBar("menu", menu));
    }
}
