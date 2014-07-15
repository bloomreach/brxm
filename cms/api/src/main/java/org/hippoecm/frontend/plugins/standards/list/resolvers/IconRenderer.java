/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugins.standards.icon.BrowserStyle;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconRenderer implements IListCellRenderer<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(IconRenderer.class);

    public Component getRenderer(String id, IModel<Node> model) {
        return new IconContainer(id, getResourceReference(model));
    }

    public IObservable getObservable(IModel<Node> model) {
        return null;
    }

    private ResourceReference getResourceReference(IModel<Node> nodeModel) {
        Node node = nodeModel.getObject();
        if (node != null) {
            try {
                return getResourceReference(node);
            } catch (RepositoryException ex) {
                log.error("Unable to determine icon for document", ex);
            }
        }
        return null;
    }

    protected ResourceReference getResourceReference(Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            if (node.hasNode(node.getName())) {
                Node child = node.getNode(node.getName());
                String nodeTypeIconName = StringUtils.replace(child.getPrimaryNodeType().getName(), ":", "-");
                return BrowserStyle.getIcon(nodeTypeIconName, "document", IconSize.TINY);
            }
            return BrowserStyle.getIcon("document", IconSize.TINY);
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            if (node instanceof HippoNode) {
                Node canonical;
                try {
                    canonical = ((HippoNode) node).getCanonicalNode();
                    if (canonical == null) {
                        return BrowserStyle.getIcon("folder-virtual", IconSize.TINY);
                    }
                } catch (ItemNotFoundException ex) {
                    return BrowserStyle.getIcon("alert", IconSize.TINY);
                }
                Node parent = canonical.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (!canonical.isSame(node)) {
                        return BrowserStyle.getIcon("document-virtual", IconSize.TINY);
                    } else {
                        String nodeTypeIconName = StringUtils.replace(node.getPrimaryNodeType().getName(), ":", "-");
                        return BrowserStyle.getIcon(nodeTypeIconName, "document", IconSize.TINY);
                    }
                }
            } else {
                Node parent = node.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    String nodeTypeIconName = StringUtils.replace(node.getPrimaryNodeType().getName(), ":", "-");
                    return BrowserStyle.getIcon(nodeTypeIconName, "document", IconSize.TINY);
                }
            }
        }

        String type = node.getPrimaryNodeType().getName();
        if (type.equals("hipposysedit:templatetype")) {
            return BrowserStyle.getIcon("document", IconSize.TINY);
        }
        return BrowserStyle.getIcon("folder", IconSize.TINY);
    }

    private static class IconContainer extends Panel {
        private static final long serialVersionUID = 1L;

        IconContainer(String id, ResourceReference resourceRef) {
            super(id);
            add(new CachingImage("icon", resourceRef));
        }
    }

}
