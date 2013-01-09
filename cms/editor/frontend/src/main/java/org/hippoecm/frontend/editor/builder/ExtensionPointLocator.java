/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.builder;

import java.io.Serializable;

import org.hippoecm.frontend.model.IModelReference;

public class ExtensionPointLocator implements Serializable {

    private final IModelReference<String> selectedPluginService;
    private ILayoutAware layout;

    public ExtensionPointLocator(IModelReference<String> selectedPluginService) {

        this.selectedPluginService = selectedPluginService;
    }

    public String getSelectedExtensionPoint() {
        // first try, use selected plugin
        String pluginId = selectedPluginService.getModel().getObject();
        if (pluginId != null) {
            String container = getContainer(pluginId);
            if (container != null) {
                return container;
            }
        }

        // second try, use default selected plugin
        pluginId = getDefaultSelectedPlugin();
        String container = getContainer(pluginId);
        if (container != null) {
            return container;
        }

        // fall back to default extension point name
        return "${cluster.id}.field";
    }

    public void setLayoutAwareRoot(ILayoutAware layout) {
        this.layout = layout;
    }

    public String getDefaultSelectedPlugin() {
        while (layout.getDefaultChild() != null) {
            layout = layout.getDefaultChild();
        }
        return layout.getTemplateBuilderPluginId();
    }

    private String getContainer(final String pluginId) {
        ContainerLocator visitor = new ContainerLocator(pluginId);
        visitor.visit(layout);
        if (visitor.found) {
            return visitor.container;
        } else {
            return null;
        }
    }

    private static class ContainerLocator {

        boolean found = false;
        String container;
        private String pluginId;

        ContainerLocator(String pluginId) {
            this.pluginId = pluginId;
        }

        void visit(ILayoutAware layout) {
            if (!found && layout.getTemplateBuilderExtensionPoint() != null) {
                container = layout.getTemplateBuilderExtensionPoint();
            }
            if (pluginId.equals(layout.getTemplateBuilderPluginId())) {
                found = true;
            } else {
                for (ILayoutAware child : layout.getChildren()) {
                    visit(child);
                }
            }
        }
    }

}
