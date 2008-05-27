/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.sa.template.impl;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.IPluginControl;
import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.service.Message;
import org.hippoecm.frontend.sa.service.render.ModelReference;
import org.hippoecm.frontend.sa.service.render.RenderService;
import org.hippoecm.frontend.sa.service.render.ModelReference.ModelMessage;
import org.hippoecm.frontend.sa.service.topic.TopicService;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.ITemplateStore;
import org.hippoecm.frontend.sa.template.ITypeStore;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine implements ITemplateEngine {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private IPluginContext context;
    private ITypeStore typeStore;
    private ITemplateStore templateStore;
    private String serviceId;
    private int templateCount = 0;

    public TemplateEngine(IPluginContext context, String serviceId, ITypeStore typeStore, ITemplateStore templateStore) {
        this.context = context;
        this.typeStore = typeStore;
        this.templateStore = templateStore;
        this.serviceId = serviceId;
    }

    public TypeDescriptor getType(String type) {
        return typeStore.getTypeDescriptor(type);
    }

    public TypeDescriptor getType(IModel model) {
        if (model instanceof JcrNodeModel) {
            try {
                JcrNodeModel nodeModel = (JcrNodeModel) model;
                return getType(nodeModel.getNode().getPrimaryNodeType().getName());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Unable to resolve type of {}", model);
        }
        return null;
    }

    public IClusterConfig getTemplate(TypeDescriptor type, String mode) {
        String templateId = serviceId + "." + (templateCount++);
        return new JavaTemplateConfig(templateStore.getTemplate(type, mode), serviceId, templateId);
    }

    public IPluginControl start(final IClusterConfig template, final IModel model) {
        String modelId = template.getString(RenderService.MODEL_ID);
        final TopicService topic = new TopicService(modelId) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onPublish(Message message) {
                switch (message.getType()) {
                case ModelReference.GET_MODEL:
                    publish(new ModelMessage(ModelReference.SET_MODEL, model));
                }
            }
        };
        topic.init(context);

        return context.start(template);
    }
}
