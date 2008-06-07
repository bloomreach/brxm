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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.ITypeStore;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine implements ITemplateEngine {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private IPluginContext context;
    private ITypeStore typeStore;
    private String serviceId;

    public TemplateEngine(IPluginContext context, ITypeStore typeStore) {
        this.context = context;
        this.typeStore = typeStore;
    }

    public void setId(String serviceId) {
        this.serviceId = serviceId;
    }

    public TypeDescriptor getType(String type) {
        return typeStore.getTypeDescriptor(type);
    }

    public TypeDescriptor getType(IModel model) {
        if (model instanceof JcrNodeModel) {
            try {
                Node node = ((JcrNodeModel) model).getNode();
                // prototype has primary type "nt:base"; look real type
                // up by finding the containing templatetype.
                if (node.isNodeType("hippo:remodel")) {
                    Node parent = node.getParent();
                    while (parent != null) {
                        if (parent.isNodeType("hippo:templatetype")) {
                            return getType(parent.getName());
                        }
                        parent = parent.getParent();
                    }
                    return null;
                }
                return getType(node.getPrimaryNodeType().getName());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Unable to resolve type of {}", model);
        }
        return null;
    }

    public IClusterConfig getTemplate(TypeDescriptor type, String mode) {
        IPluginConfigService configService = context.getService("service.plugin.config", IPluginConfigService.class);
        IClusterConfig cluster = configService.getPlugins("template/" + type.getName() + "/" + mode);
        if (cluster != null) {
            cluster.put(ITemplateEngine.ENGINE, serviceId);
        }
        return cluster;
    }

}
