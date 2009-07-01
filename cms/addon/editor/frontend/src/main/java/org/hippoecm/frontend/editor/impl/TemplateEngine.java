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
package org.hippoecm.frontend.editor.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.tools.JcrPrototypeStore;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine implements ITemplateEngine {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    private IStore<ITypeDescriptor> typeStore;
    private IStore<IClusterConfig> jcrTemplateStore;
    private IStore<IClusterConfig> builtinTemplateStore;
    private JcrPrototypeStore prototypeStore;

    public TemplateEngine() {
        final IStore<ITypeDescriptor> jcrTypeStore = new JcrTypeStore();
        final IStore<ITypeDescriptor> builtinTypeStore = new BuiltinTypeStore();
        typeStore = new IStore<ITypeDescriptor>() {
            private static final long serialVersionUID = 1L;

            public void close() {
            }

            public void delete(ITypeDescriptor object) throws StoreException {
                throw new UnsupportedOperationException("Type store is read only");
            }

            public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) {
                Map<String, ITypeDescriptor> types = new HashMap<String, ITypeDescriptor>();

                for (Iterator<ITypeDescriptor> iter = builtinTypeStore.find(criteria); iter.hasNext();) {
                    ITypeDescriptor type = iter.next();
                    types.put(type.getName(), type);
                }
                for (Iterator<ITypeDescriptor> iter = jcrTypeStore.find(criteria); iter.hasNext();) {
                    ITypeDescriptor type = iter.next();
                    types.put(type.getName(), type);
                }
                return types.values().iterator();
            }

            public ITypeDescriptor load(String id) throws StoreException {
                try {
                    return jcrTypeStore.load(id);
                } catch (StoreException ex) {
                    return builtinTypeStore.load(id);
                }
            }

            public String save(ITypeDescriptor object) throws StoreException {
                throw new UnsupportedOperationException("Type store is read only");
            }

        };

        this.jcrTemplateStore = new JcrTemplateStore(typeStore);
        this.builtinTemplateStore = new BuiltinTemplateStore(typeStore);
        this.prototypeStore = new JcrPrototypeStore();
    }

    public ITypeDescriptor getType(String type) throws TemplateEngineException {
        try {
            return typeStore.load(type);
        } catch (StoreException ex) {
            throw new TemplateEngineException("Unable to load type", ex);
        }
    }

    public ITypeDescriptor getType(IModel model) throws TemplateEngineException {
        if (model instanceof JcrNodeModel) {
            try {
                Node node = ((JcrNodeModel) model).getNode();
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
        }
        throw new TemplateEngineException("Unable to resolve type of " + model);
    }

    public IClusterConfig getTemplate(ITypeDescriptor type, String mode) throws TemplateEngineException {
        Map<String, Object> criteria = new MiniMap(2);
        criteria.put("type", type);
        criteria.put("mode", mode);
        Iterator<IClusterConfig> iter = jcrTemplateStore.find(criteria);
        if (iter.hasNext()) {
            return iter.next();
        }
        iter = builtinTemplateStore.find(criteria);
        if (iter.hasNext()) {
            return iter.next();
        }
        throw new TemplateEngineException("No template found");
    }

    public IModel getPrototype(ITypeDescriptor type) {
        return prototypeStore.getPrototype(type.getName(), false);
    }

}
