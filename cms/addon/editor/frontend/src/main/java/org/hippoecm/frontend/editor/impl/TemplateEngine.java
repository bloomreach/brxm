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

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.tools.JcrPrototypeStore;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
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
    private IStore<IClusterConfig> templateStore;
    private JcrPrototypeStore prototypeStore;

    public TemplateEngine(IPluginContext context) {
        this.typeStore = new JcrTypeStore(context);

        this.templateStore = new JcrTemplateStore(typeStore, context);
        this.prototypeStore = new JcrPrototypeStore();
    }

    public ITypeDescriptor getType(String type) {
        return typeStore.load(type);
    }

    public ITypeDescriptor getType(IModel model) {
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
                        return null;
                    }
                } else if (node.isNodeType("nt:frozenNode")) {
                    String type = node.getProperty("jcr:frozenPrimaryType").getString();
                    return getType(type);
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

    public IClusterConfig getTemplate(ITypeDescriptor type, String mode) {
        Map<String, Object> criteria = new MiniMap(2);
        criteria.put("type", type);
        criteria.put("mode", mode);
        Iterator<IClusterConfig> iter = templateStore.find(criteria);
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    public IModel getPrototype(ITypeDescriptor type) {
        return prototypeStore.getPrototype(type.getName(), false);
    }

}
