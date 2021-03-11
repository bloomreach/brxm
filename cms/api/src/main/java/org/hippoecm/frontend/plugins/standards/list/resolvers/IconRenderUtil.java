/*
 * Copyright 2014-2021 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.standards.icon.BrowserStyle;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Utility class for rendering icons.
 */
public class IconRenderUtil {

    public static final IconSize DEFAULT_SIZE = IconSize.L;

    private IconRenderUtil() {
    }

    public static HippoIcon getDocumentOrFolderIcon(final String id, final Node node) throws RepositoryException {
        return getDocumentOrFolderIcon(id, node, DEFAULT_SIZE);
    }

    public static HippoIcon getDocumentOrFolderIcon(final String id, final Node node, IconSize size) throws RepositoryException {
        if (size == null) {
            size = DEFAULT_SIZE;
        }

        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return getIconForHandle(id, node, size);
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            if (node instanceof HippoNode) {
                Node canonical;
                try {
                    canonical = ((HippoNode) node).getCanonicalNode();
                    if (canonical == null) {
                        return HippoIcon.fromSprite(id, Icon.FOLDER, size);
                    }
                } catch (ItemNotFoundException ex) {
                    return HippoIcon.fromSprite(id, Icon.EMPTY);
                }
                Node parent = canonical.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (!canonical.isSame(node)) {
                        return HippoIcon.fromSprite(id, getDocumentIcon(canonical), size);
                    } else {
                        return getIconForNodeType(id, node.getPrimaryNodeType(), getDocumentIcon(parent), size);
                    }
                }
            } else {
                Node parent = node.getParent();
                if (parent != null && parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    return getIconForNodeType(id, node.getPrimaryNodeType(), getDocumentIcon(parent), size);
                }
            }
        }

        String type = node.getPrimaryNodeType().getName();
        if (type.equals(HippoNodeType.NT_TEMPLATETYPE)) {
            return HippoIcon.fromSprite(id, Icon.FILE_TEXT, size);
        }
        return HippoIcon.fromSprite(id, getFolderIcon(node), size);
    }

    private static final Icon getFolderIcon(Node folder) throws RepositoryException {
        return folder.isNodeType(HippoStdNodeType.NT_XPAGE_FOLDER) ? Icon.XPAGE_FOLDER : Icon.FOLDER;
    }

    private static final Icon getDocumentIcon(Node document) throws RepositoryException {
        return document.isNodeType("hst:xpagemixin") ? Icon.XPAGE_DOCUMENT : Icon.FILE_TEXT;
    }

    private static HippoIcon getIconForHandle(final String id, final Node node, final IconSize size) throws RepositoryException {
        if (node.hasNode(node.getName())) {
            Node child = node.getNode(node.getName());
            return getIconForNodeType(id, child.getPrimaryNodeType(), getDocumentIcon(child), size);
        }
        return HippoIcon.fromSprite(id, Icon.FILE_TEXT, size);
    }

    public static HippoIcon getIconForNodeType(final String id, final NodeType type, final Icon defaultIcon, final IconSize size) {
        final String nodeTypeIconName = StringUtils.replace(type.getName(), ":", "-");
        final ResourceReference reference = BrowserStyle.getIconOrNull(nodeTypeIconName, size);
        if (reference != null) {
            final HippoIcon icon = HippoIcon.fromResource(id, reference, size);
            icon.addCssClass("hi");
            icon.addCssClass("hi-custom-node-type");
            icon.addCssClass("hi-" + size.name().toLowerCase());
            return icon;
        }
        return HippoIcon.fromSprite(id, defaultIcon, size);
    }

}
