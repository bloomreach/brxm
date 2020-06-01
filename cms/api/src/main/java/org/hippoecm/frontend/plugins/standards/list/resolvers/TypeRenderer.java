/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;

import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_TEMPLATETYPE;

public class TypeRenderer extends AbstractNodeRenderer {

    private static final TypeRenderer INSTANCE = new TypeRenderer();

    private TypeRenderer() {
    }

    public static TypeRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    protected Component getViewer(String id, Node node) throws RepositoryException {
        return new Label(id, getLabelModel(node));
    }

    private IModel<String> getLabelModel(Node node) throws RepositoryException {
        if (node.isNodeType(NT_HANDLE) && node.hasNode(node.getName())) {
            node = node.getNode(node.getName());
        }
        String type = null;
        if (node.isNodeType(NT_DOCUMENT)) {
            type = node.getPrimaryNodeType().getName();
        }
        if (node.isNodeType(NT_TEMPLATETYPE)) {
            type = node.getParent().getName() + ":" + node.getName();
        }
        if (type != null) {
            return new TypeTranslator(new JcrNodeTypeModel(type)).getTypeName();
        }
        return new Model<>("unknown");
    }

}
