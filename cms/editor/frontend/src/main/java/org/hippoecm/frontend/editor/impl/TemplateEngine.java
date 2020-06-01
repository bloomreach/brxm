/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.impl;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.prototype.IPrototypeStore;
import org.hippoecm.editor.template.ITemplateLocator;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine implements ITemplateEngine, IDetachable {

    static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private ITypeLocator typeLocator;
    private ITemplateLocator templateLocator;
    private IPrototypeStore prototypeStore;
    private EditableTypes editableTypes;

    public TemplateEngine(ITypeLocator typeLocator, IPrototypeStore prototypeStore,
            ITemplateLocator templateLocator) {
        this.typeLocator = typeLocator;
        this.prototypeStore = prototypeStore;
        this.templateLocator = templateLocator;
    }

    @Override
    public ITypeDescriptor getType(String type) throws TemplateEngineException {
        try {
            return typeLocator.locate(type);
        } catch (StoreException ex) {
            throw new TemplateEngineException("Unable to load type", ex);
        }
    }

    @Override
    public ITypeDescriptor getType(IModel model) throws TemplateEngineException {
        if (model instanceof JcrNodeModel) {
            try {
                Node node = ((JcrNodeModel) model).getNode();
                if (node == null) {
                    throw new TemplateEngineException("Invalid model, node does not exist");
                }
                // prototype has primary type "nt:unstructured"; look up real type
                // by finding the containing templatetype.
                if (node.getPath().startsWith("/hippo:namespaces")
                        && node.getName().equals(HippoNodeType.HIPPO_PROTOTYPE)) {
                    if (node.isNodeType("nt:unstructured")) {
                        Node parent = node.getParent();
                        while (parent != null) {
                            if (parent.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                                return getType(parent.getParent().getName() + ":"
                                        + NodeNameCodec.decode(parent.getName()));
                            }
                            parent = parent.getParent();
                        }
                        throw new TemplateEngineException("Could not find template type ancestor");
                    }
                } else if (node.isNodeType("nt:frozenNode")) {
                    String type = node.getProperty("jcr:frozenPrimaryType").getString();
                    return getType(type);
                }
                return getType(node.getPrimaryNodeType().getName());
            } catch (RepositoryException ex) {
                throw new TemplateEngineException("Invalid model", ex);
            }
        } else if (model instanceof JcrPropertyValueModel) {
            int type = ((JcrPropertyValueModel) model).getType();
            final String typeName = PropertyType.nameFromValue(type);
            return getType(typeName);
        }
        throw new TemplateEngineException("Unable to resolve type of " + model);
    }

    @Override
    public IClusterConfig getTemplate(ITypeDescriptor type, Mode mode) throws TemplateEngineException {
        final Map<String, Object> criteria = new MiniMap<>(2);
        criteria.put("type", type);
        criteria.put("mode", mode.toString());
        try {
            return templateLocator.getTemplate(criteria);
        } catch (StoreException e) {
            throw new TemplateEngineException("Error locating template", e);
        }
    }

    @Override
    public IModel getPrototype(ITypeDescriptor type) {
        return prototypeStore.getPrototype(type.getName(), false);
    }

    @Override
    public List<String> getEditableTypes() {
        if (editableTypes == null) {
            editableTypes = new EditableTypes();
        }
        return editableTypes;
    }

    @Override
    public void detach() {
        if (templateLocator instanceof IDetachable) {
            ((IDetachable) templateLocator).detach();
        }
        if (prototypeStore instanceof IDetachable) {
            ((IDetachable) prototypeStore).detach();
        }
        typeLocator.detach();
        editableTypes = null;
    }

}
