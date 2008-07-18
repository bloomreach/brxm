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
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.IJcrNodeViewerFactory;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconResolver implements IJcrNodeViewerFactory {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(IconResolver.class);

    public Component getViewer(String id, JcrNodeModel model) {
        String cssClass = null;
        try {
            HippoNode n = (HippoNode) model.getObject();
            String type = null;
            if (n.isNodeType(HippoNodeType.NT_HANDLE)) {
                type = n.getPrimaryNodeType().getName();
                NodeIterator nodeIt = n.getNodes();
                while (nodeIt.hasNext()) {
                    Node childNode = nodeIt.nextNode();
                    if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        type = childNode.getPrimaryNodeType().getName();
                        break;
                    }
                }
                if (type.indexOf(":") > -1) {
                    cssClass = "document-16";
                }
            } else {
                cssClass = "folder-16";
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        Component result = new Label(id);
        if (cssClass != null) {
            result.add(new AttributeModifier("class", true, new CssModel(cssClass)));
        }
        return result;
    }
    
    
    private class CssModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;
        
        private String cssClass;
        
        public CssModel(String cssClass) {
            this.cssClass = cssClass;
        }

        @Override
        public Object getObject() {
            return cssClass;
        }
    };

}
