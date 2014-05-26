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
package org.hippoecm.frontend.service.render;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderService<T> extends AbstractRenderService<T> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

    public RenderService(IPluginContext context, IPluginConfig properties) {
        super(context, properties);
    }

    @Override
    protected ExtensionPoint createExtensionPoint(String extension) {
        return new ExtensionPoint(extension);
    }

    /**
     * Extension point that can add the extension to a child component.  The name
     * of the extension will be the full (relative) path of the extension.  The last
     * part of the path, i.e. the id, is used to get the service id from the plugin
     * configuration.
     */
    protected class ExtensionPoint extends AbstractRenderService<T>.ExtensionPoint {
        private static final long serialVersionUID = 1L;

        private String path;
        private String id;

        protected ExtensionPoint(String extension) {
            super(extension);

            path = "";
            id = extension;
            if (extension.indexOf(':') > 0) {
                path = extension.substring(0, extension.lastIndexOf(':'));
                id = extension.substring(extension.lastIndexOf(':') + 1);
            }
            addPanel();
        }

        void addPanel() {
            if (get(path) == null) {
                log.debug("Not adding empty panel to future child component");
                return;
            }
            ((MarkupContainer) get(path)).add(new EmptyPanel(id));
        }

        @Override
        protected void register() {
            IPluginConfig config = getPluginConfig();
            IPluginContext context = getPluginContext();
            if (config.containsKey(id)) {
                context.registerTracker(this, config.getString(id));
            } else {
                log.debug("Extension {} has not been configured; not registering tracker", extension);
            }
        }

        @Override
        protected void unregister() {
            IPluginConfig config = getPluginConfig();
            IPluginContext context = getPluginContext();
            if (config.containsKey(id)) {
                context.unregisterTracker(this, config.getString(id));
            }
        }

        @Override
        public void onServiceAdded(IRenderService service, String name) {
            service.bind(RenderService.this, id);
            ((MarkupContainer) get(path)).addOrReplace(service.getComponent());
            redraw();
            super.onServiceAdded(service, name);
        }

        @Override
        public void onRemoveService(IRenderService service, String name) {
            ((MarkupContainer) get(path)).replace(new EmptyPanel(id));
            service.unbind();
            redraw();
            super.onRemoveService(service, name);
        }
    }
}
