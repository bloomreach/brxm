/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.browser;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.plugins.console.icons.IconLabel;
import org.hippoecm.frontend.plugins.console.icons.JcrNodeIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

public final class NodeIconUtils {

    /**
     * Create a component representing the icon for a node tree. What icon to show and
     * whether or not it has a tooltip depends on the underlying JCR node.
     *
     * @param id   wicket ID of the component
     * @param node of the tree for which to create the icon
     * @return     a node-specific icon, or a default icon
     */
    public static Component createJcrNodeIcon(final String id, final TreeNode node) {
        final IModel<Node> nodeModel = ((IJcrTreeNode) node).getNodeModel();
        if (nodeModel ==  null) {
            return createJcrNodeIcon(id, (Node) null);
        }
        return createJcrNodeIcon(id, nodeModel.getObject());
    }

    /**
     * Create a component representing the icon for a node tree. What icon to show and
     * whether or not it has a tooltip depends on the underlying JCR node.
     *
     * @param id    wicket ID of the component
     * @param model generic model for which a tree icon is generated
     * @return      a node-specific icon, or a default icon
     */
    public static Component createJcrNodeIcon(final String id, final IModel<Node> model) {
        if (model != null) {
            return createJcrNodeIcon(id, model.getObject());                
        }
        return createJcrNodeIcon(id, (Node) null);
    }

    /**
     * Create a component representing the icon for a node tree. What icon to show and
     * whether or not it has a tooltip depends on the underlying JCR node.
     *
     * @param id   wicket ID of the component
     * @param node jcr node for which to create the icon
     * @return     a node-specific icon, or a default icon
     */
    public static Component createJcrNodeIcon(final String id, final Node node) {
        if (node ==  null) {
            return createUnknownNodeIcon(id);
        }

        final Label icon = new Label(id, StringUtils.EMPTY);
        icon.add(CssClass.append(JcrNodeIcon.getIconCssClass(node)));

        final String tooltip = determineNodeTooltip(node);
        if (StringUtils.isNotBlank(tooltip)) {
            icon.add(TitleAttribute.append(tooltip));
        }
        return icon;
    }

    private static Component createUnknownNodeIcon(final String id) {
        return new IconLabel(id, JcrNodeIcon.FA_UNKNOWN_NODE_CSS_CLASS);
    }

    private static String determineNodeTooltip(final Node jcrNode) {
        try {
            if (jcrNode.hasProperty("hippostd:state")) {
                return jcrNode.getProperty("hippostd:state").getString();
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return null;
    }
}
