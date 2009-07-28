/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.validator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.MarkupStream;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrValidatorPlugin implements IPlugin, IValidateService, IFeedbackMessageFilter {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(JcrValidatorPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;
    private Component component;
    private transient boolean validated = false;
    private transient boolean isvalid = true;

    public JcrValidatorPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;
        this.component = new Component("component") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onRender(MarkupStream markupStream) {
                throw new UnsupportedOperationException();
            }
        };

        if (config.getString(IValidateService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.info("No validator id {} defined", IValidateService.VALIDATE_ID);
        }

        if (config.getString(RenderService.FEEDBACK) != null) {
            context.registerService(this, config.getString(RenderService.FEEDBACK));
        } else {
            log.info("No feedback id {} defined", RenderService.FEEDBACK);
        }
    }

    public boolean accept(FeedbackMessage message) {
        return message.getReporter() == component;
    }

    public boolean hasError() {
        if (!validated) {
            validate();
        }
        return !isvalid;
    }

    public void validate() {
        isvalid = true;
        IModelReference modelRef = context.getService(config.getString(RenderService.MODEL_ID), IModelReference.class);
        JcrNodeModel nodeModel = (JcrNodeModel) modelRef.getModel();
        try {
            validateNode(nodeModel.getNode());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            component.error("Problem while validating: " + ex.getMessage());
            isvalid = false;
        }
        validated = true;
    }

    void validateNode(Node node) throws RepositoryException {
        NodeType primary = node.getPrimaryNodeType();
        validateNodeType(node, primary);

        NodeType[] mixins = node.getMixinNodeTypes();
        for (NodeType mixin : mixins) {
            validateNodeType(node, mixin);
        }
    }
    
    void validateNodeType(Node node, NodeType type) throws RepositoryException {
        for (PropertyDefinition propertyDefinition : type.getPropertyDefinitions()) {
            if (propertyDefinition.isMandatory()) {
                String propName = propertyDefinition.getName();
                if (node.hasProperty(propName)) {
                    Property mandatory = node.getProperty(propName);
                    if (mandatory.getDefinition().isMultiple()) {
                        for (Value value : mandatory.getValues()) {
                            if (value.getString() == null || value.getString().equals("")) {
                                isvalid = false;
                                component.error("Mandatory field " + propName + " has no value.");
                                break;
                            }
                        }
                    } else {
                        Value value = mandatory.getValue();
                        if (value == null || value.getString() == null || value.getString().equals("")) {
                            isvalid = false;
                            component.error("Mandatory field " + propName + " has no value.");
                            break;
                        }
                    }
                } else {
                    isvalid = false;
                    component.error("Mandatory field " + propName + " has no value.");
                    break;
                }
            }
        }
        for (NodeDefinition definition : type.getChildNodeDefinitions()) {
            if (definition.isMandatory()) {
                String nodeName = definition.getName();
                if (!"*".equals(nodeName)) {
                    NodeIterator nodes = node.getNodes(nodeName);
                    if (!nodes.hasNext()) {
                        component.error("Mandatory field " + nodeName + "is not present");
                    }
                }
            }
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            validateNode(child);
        }
    }

}
