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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderService;

public class ListRenderService extends AbstractRenderService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListRenderService.class);

    public ListRenderService(IPluginContext context, IPluginConfig properties) {
        super(context, properties);
    }

    protected ExtensionPoint createExtensionPoint(String extension) {
        return new ExtensionPoint(extension);
    }

    protected class ExtensionPoint extends AbstractRenderService.ExtensionPoint {
        private static final long serialVersionUID = 1L;


        ExtensionPoint(String extension) {
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
