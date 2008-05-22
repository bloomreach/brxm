/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.plugin.editor;

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.ITitleDecorator;
import org.hippoecm.frontend.service.editor.EditorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorPlugin extends EditorService implements IPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditorPlugin.class);

    private TitleDecorator title;

    public void start(IPluginContext context) {
        IPluginConfig properties = context.getProperties();
        init(context, properties);

        String decoratorId = getServiceId() + ".decorator";
        if (decoratorId != null) {
            title = new TitleDecorator();
            context.registerService(title, decoratorId);
        } else {
            title = null;
            log.warn("No decorator id was found");
        }
    }

    public void stop() {
        IPluginContext context = getPluginContext();

        if (title != null) {
            context.unregisterService(title, getServiceId() + ".decorator");
            title = null;
        }
        destroy();
    }

    class TitleDecorator implements ITitleDecorator, IClusterable {
        private static final long serialVersionUID = 1L;

        public String getTitle() {
            JcrNodeModel model = (JcrNodeModel) getModel();
            try {
                return model.getNode().getDisplayName();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return "";
        }
    }
}
