/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.standards.icon.BrowserStyle;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Utility class for rendering icons.
 */
public class IconRenderUtil {

    private IconRenderUtil() {
    }

    public static HippoIcon getDocumentOrFolderIcon(final String id, final Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return getIconForHandle(id, node);
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            if (node instanceof HippoNode) {
                Node canonical;
                try {
                    canonical = ((HippoNode) node).getCanonicalNode();
                    if (canonical == null) {
                        return HippoIcon.fromSprite(id, Icon.FOLDER, IconSize.L);
                    }
                } catch (ItemNotFoundException ex) {
                    return HippoIcon.fromSprite(id, Icon.EMPTY);
                }
                Node parent = canonical.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (!canonical.isSame(node)) {
                        return HippoIcon.fromSprite(id, Icon.FILE_TEXT, IconSize.L);
                    } else {
                        return getIconForNodeType(id, node.getPrimaryNodeType(), Icon.FILE_TEXT, IconSize.L);
                    }
                }
            } else {
                Node parent = node.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    return getIconForNodeType(id, node.getPrimaryNodeType(), Icon.FILE_TEXT, IconSize.L);
                }
            }
        }

        String type = node.getPrimaryNodeType().getName();
        if (type.equals(HippoNodeType.NT_TEMPLATETYPE)) {
            return HippoIcon.fromSprite(id, Icon.FILE_TEXT, IconSize.L);
        }
        return HippoIcon.fromSprite(id, Icon.FOLDER, IconSize.L);
    }

    private static HippoIcon getIconForHandle(final String id, final Node node) throws RepositoryException {
        if (node.hasNode(node.getName())) {
            Node child = node.getNode(node.getName());
            return getIconForNodeType(id, child.getPrimaryNodeType(), Icon.FILE_TEXT, IconSize.L);
        }
        return HippoIcon.fromSprite(id, Icon.FILE_TEXT, IconSize.L);
    }

    public static HippoIcon getIconForNodeType(final String id, final NodeType type, final Icon defaultIcon, final IconSize size) {
        final String nodeTypeIconName = StringUtils.replace(type.getName(), ":", "-");
        final ResourceReference reference = BrowserStyle.getIconOrNull(nodeTypeIconName, size);
        if (reference != null) {
            return HippoIcon.fromResource(id, reference);
        }
        return HippoIcon.fromSprite(id, defaultIcon);
    }

}
