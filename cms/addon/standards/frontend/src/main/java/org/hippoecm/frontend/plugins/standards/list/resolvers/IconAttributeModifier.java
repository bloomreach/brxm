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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconAttributeModifier extends AbstractNodeAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(IconAttributeModifier.class);

    private static class IconAttributeModel extends LoadableDetachableModel {
        private static final long serialVersionUID = 1L;

        private JcrNodeModel nodeModel;

        IconAttributeModel(JcrNodeModel model) {
            this.nodeModel = model;
        }

        @Override
        public void detach() {
            super.detach();
            nodeModel.detach();
        }
        
        protected Object load() {
            Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    String type = node.getPrimaryNodeType().getName();
    
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        NodeIterator nodeIt = node.getNodes();
                        while (nodeIt.hasNext()) {
                            Node childNode = nodeIt.nextNode();
                            if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                type = childNode.getPrimaryNodeType().getName();
                                break;
                            }
                        }
                        if (type.indexOf(":") > -1) {
                            return "document-16";
                        }
                    }
                    if (type.equals("hippo:templatetype")) {
                        return "document-16";
                    }
                    return "folder-16";
                } catch (RepositoryException ex) {
                    log.error("Unable to determine icon for document", ex);
                }
            }
            return null;
        }
    }

    @Override
    public AttributeModifier getCellAttributeModifier(Node node) {
        return new CssClassAppender(new IconAttributeModel(new JcrNodeModel(node)));
    }

    @Override
    public AttributeModifier getColumnAttributeModifier(Node node) {
        return new CssClassAppender(new Model("icon-16"));
    }

}
