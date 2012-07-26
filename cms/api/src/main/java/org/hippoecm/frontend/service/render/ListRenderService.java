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
package org.hippoecm.frontend.service.render;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;

public class ListRenderService extends AbstractRenderService<Void> {

    private static final long serialVersionUID = 1L;

    public ListRenderService(IPluginContext context, IPluginConfig properties) {
        super(context, properties);
    }

    @Override
    protected ExtensionPoint createExtensionPoint(String extension) {
        return new ExtensionPoint(extension);
    }

    protected class ExtensionPoint extends AbstractRenderService<Void>.ExtensionPoint {
        private static final long serialVersionUID = 1L;


        protected ExtensionPoint(String extension) {
            super(extension);
        }

        @Override
        public void onServiceAdded(IRenderService service, String name) {
            service.bind(ListRenderService.this, "id");
            service.getComponent().setVisible(false);
            super.onServiceAdded(service, name);
        }

        @Override
        public void onRemoveService(IRenderService service, String name) {
            service.unbind();
            super.onRemoveService(service, name);
        }
    }
}
