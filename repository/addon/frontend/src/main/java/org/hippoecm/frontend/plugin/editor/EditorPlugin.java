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
package org.hippoecm.frontend.plugin.editor;

import java.io.Serializable;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.core.IPluginConfig;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IDialogService;
import org.hippoecm.frontend.service.IDynamicService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.editor.EditorService;
import org.hippoecm.frontend.util.ServiceTracker;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorPlugin extends EditorService implements Plugin, IDynamicService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditorPlugin.class);

    private ServiceTracker factory;
    private TitleDecorator title;

    public EditorPlugin() {
        factory = new ServiceTracker(IFactoryService.class);
    }

    public void start(PluginContext context) {
        IPluginConfig properties = context.getProperties();
        factory.open(context, properties.get(Plugin.FACTORY_ID).getStrings().get(0));
        init(context, properties.get(Plugin.SERVICE_ID).getStrings().get(0), properties);

        String decoratorId = getDecoratorId();
        if (decoratorId != null) {
            title = new TitleDecorator();
            context.registerService(title, decoratorId);
        } else {
            title = null;
            log.warn("No decorator id was found");
        }
    }

    public void stop() {
        PluginContext context = getPluginContext();

        if (title != null) {
            context.unregisterService(title, getDecoratorId());
            title = null;
        }
        destroy();
        factory.close();
    }

    public void delete() {
        IDialogService dialogService = getDialogService();
        try {
            Node node = ((JcrNodeModel) getModel()).getNode();
            HippoSession session = (HippoSession) node.getSession();
            if (session.pendingChanges(node, "nt:base").hasNext()) {
                dialogService.show(new OnCloseDialog(dialogService, (JcrNodeModel) getModel(), this));
            } else {
                deleteEditor();
            }
        } catch (RepositoryException e) {
            dialogService.show(new ExceptionDialog(dialogService, e.getMessage()));
            log.info(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    void deleteEditor() {
        List<Serializable> services = factory.getServices();
        if (services.size() == 1) {
            IFactoryService service = (IFactoryService) services.get(0);
            service.delete(this);
        }
    }

    class TitleDecorator implements ITitleDecorator, Serializable {
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
