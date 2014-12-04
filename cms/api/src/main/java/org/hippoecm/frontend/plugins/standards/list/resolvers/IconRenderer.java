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
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugins.standards.icon.BrowserStyle;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconRenderer implements IListCellRenderer<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(IconRenderer.class);

    public Component getRenderer(String id, IModel<Node> model) {
        final Node node = model.getObject();
        if (node == null) {
            log.warn("Using default icon for unknown node");
            return defaultIcon(id);
        }
        try {
            return getIcon(id, node);
        } catch (RepositoryException e) {
            log.warn("Unable to determine icon for node '{}', using default icon instead",
                    JcrUtils.getNodePathQuietly(node), e);
            return defaultIcon(id);
        }
    }

    private HippoIcon defaultIcon(final String id) {
        return new HippoIcon(id, Icon.BULLET_LARGE);
    }

    @Override
    public IObservable getObservable(IModel<Node> model) {
        return null;
    }

    protected HippoIcon getIcon(final String id, final Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            if (node.hasNode(node.getName())) {
                Node child = node.getNode(node.getName());
                String nodeTypeIconName = StringUtils.replace(child.getPrimaryNodeType().getName(), ":", "-");
                return getIcon(id, nodeTypeIconName, Icon.DOCUMENT_SMALL, IconSize.SMALL);
            }
            return new HippoIcon(id, Icon.DOCUMENT_SMALL);
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            if (node instanceof HippoNode) {
                Node canonical;
                try {
                    canonical = ((HippoNode) node).getCanonicalNode();
                    if (canonical == null) {
                        return new HippoIcon(id, Icon.FOLDER_SMALL);
                    }
                } catch (ItemNotFoundException ex) {
                    return new HippoIcon(id, Icon.EMPTY_SMALL);
                }
                Node parent = canonical.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (!canonical.isSame(node)) {
                        return new HippoIcon(id, Icon.DOCUMENT_SMALL);
                    } else {
                        String nodeTypeIconName = StringUtils.replace(node.getPrimaryNodeType().getName(), ":", "-");
                        return getIcon(id, nodeTypeIconName, Icon.DOCUMENT_SMALL, IconSize.TINY);
                    }
                }
            } else {
                Node parent = node.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    String nodeTypeIconName = StringUtils.replace(node.getPrimaryNodeType().getName(), ":", "-");
                    return getIcon(id, nodeTypeIconName, Icon.DOCUMENT_SMALL, IconSize.TINY);
                }
            }
        }

        String type = node.getPrimaryNodeType().getName();
        if (type.equals("hipposysedit:templatetype")) {
            return new HippoIcon(id, Icon.DOCUMENT_SMALL);
        }
        return new HippoIcon(id, Icon.FOLDER_SMALL);
    }

    private HippoIcon getIcon(String id, String name, Icon defaultIcon, IconSize size) {
        ResourceReference reference = BrowserStyle.getIconOrNull(name, size);
        if (reference != null) {
            return new HippoIcon(id, reference);
        }
        return new HippoIcon(id, defaultIcon);
    }

}
