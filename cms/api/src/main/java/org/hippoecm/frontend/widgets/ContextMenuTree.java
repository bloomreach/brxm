/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.widgets;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.extensions.markup.html.tree.LinkType;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;

/**
 *
 */
public class ContextMenuTree extends DefaultAbstractTree {

    private static final long serialVersionUID = 1L;

    /**
     * use own styling
     */
    private static final ResourceReference TREE_STYLE = new CssResourceReference(JcrTree.class, "res/tree.css");

    /**
     * Reference to the icon for context menus
     */
    private static final ResourceReference MENU = new PackageResourceReference(ContextMenuTree.class, "res/menu.gif");

    /**
     * Reference to the javascript that adds hover behavior to the tree's div
     */
    private static final ResourceReference IE6_HOVER_FIX = new PackageResourceReference(ContextMenuTree.class,
            "res/ie6_hover_fix.js");

    public ContextMenuTree(String id, TreeModel model) {
        super(id, model);

        setLinkType(LinkType.AJAX);

        ITreeState treeState = getTreeState();
        treeState.setAllowSelectMultiple(false);
        treeState.collapseAll();
        treeState.expandNode(model.getRoot());
    }

    @Override
    protected ResourceReference getCSS() {
        return TREE_STYLE;
    }

    protected ResourceReference getMenuIcon(TreeNode node) {
        return MENU;
    }

    protected Component newMenuIcon(MarkupContainer parent, String id, final TreeNode node) {
        return new WebMarkupContainer(id) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("style", "background-image: url('" + RequestCycle.get().urlFor(
                        new ResourceReferenceRequestHandler(
                                getMenuIcon(node))) + "')");
            }
        };

    }

    protected MarkupContainer newContextContent(MarkupContainer parent, String id, final TreeNode node) {
        return new WebMarkupContainer(id);
    }

    protected MarkupContainer newContextLink(final MarkupContainer parent, String id, final TreeNode node,
                                             MarkupContainer content) {
        AjaxLink<Void> link = new ContextLink(id, content, parent) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // It was a agreed decision that the node being operated upon was not to be selected
                // getTreeState().selectNode(node, !getTreeState().isNodeSelected(node));
                updateTree(target);
                content.setVisible(true);
                target.add(parent);
                IContextMenuManager menuManager = findParent(IContextMenuManager.class);
                if (menuManager != null) {
                    menuManager.showContextMenu(this);
                    onContextLinkClicked(content, target);
                }
            }
        };
        setOutputMarkupId(true);
        content.setOutputMarkupId(true);
        content.setVisible(false);
        link.add(newMenuIcon(link, "menuimage", node));
        return link;
    }

    protected void onContextLinkClicked(MarkupContainer content, AjaxRequestTarget target) {
    }

    @Override
    protected void populateTreeItem(WebMarkupContainer item, final int level) {
        final TreeNode node = (TreeNode) item.getDefaultModelObject();

        item.add(newIndentation(item, "indent", (TreeNode) item.getDefaultModelObject(), level));

        item.add(newJunctionLink(item, "link", "image", node));

        MarkupContainer nodeLink = newNodeLink(item, "nodeLink", node);
        item.add(nodeLink);

        nodeLink.add(newNodeIcon(nodeLink, "icon", node));

        nodeLink.add(new Label("label", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return renderNode(node, level);
            }
        }));

        decorateNodeLink(nodeLink, node, level);

        MarkupContainer contextContent = newContextContent(item, "contextContent", node);
        item.add(contextContent);
        MarkupContainer contextLink = newContextLink(item, "contextLink", node, contextContent);
        // FIXME: and what if it _is_ null?
        if (contextLink != null) {
            item.add(contextLink);
        }

        // do distinguish between selected and unselected rows we add an
        // behavior
        // that modifies row css class.
        item.add(new Behavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(Component component, ComponentTag tag) {
                super.onComponentTag(component, tag);
                if (getTreeState().isNodeSelected(node)) {
                    tag.put("class", "row-selected");
                } else {
                    tag.put("class", "row");
                }
            }
        });
    }

    /**
     * Override this method to decorate the nodeLink
     */
    protected void decorateNodeLink(MarkupContainer nodeLink, TreeNode node, int level) {
    }

    /**
     * This method is called for every node to get it's string representation.
     *
     * @param node The tree node to get the string representation of
     * @return The string representation
     */
    protected String renderNode(TreeNode node, int level) {
        return node.toString();
    }

    @Override
    public void onTargetRespond(AjaxRequestTarget target) {
        super.onTargetRespond(target);
        WebClientInfo info = getWebSession().getClientInfo();
        if (info != null) {
            String userAgent = info.getUserAgent().toLowerCase();
            if (userAgent.indexOf("msie 6") > -1) {
                target
                        .getHeaderResponse()
                        .render(OnDomReadyHeaderItem.forScript(
                                "fixIE6Hover('row', 'div', 'context-hover'); fixIE6Hover('row-selected', 'div', 'context-hover-selected');"));
            }
        }
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        WebClientInfo info = getWebSession().getClientInfo();
        if (info != null) {
            String userAgent = info.getUserAgent().toLowerCase();
            if (userAgent.indexOf("msie 6") > -1) {
                container.getHeaderResponse().render(JavaScriptHeaderItem.forReference(IE6_HOVER_FIX));
                container
                        .getHeaderResponse()
                        .render(OnDomReadyHeaderItem.forScript(
                                "fixIE6Hover('row', 'div', 'context-hover'); fixIE6Hover('row-selected', 'div', 'context-hover-selected');"));
            }
        }
    }

    public static abstract class ContextLink extends AjaxLink<Void> implements IContextMenu {
        private static final long serialVersionUID = 1L;

        MarkupContainer content;
        MarkupContainer parent;

        public ContextLink(String id, MarkupContainer content, MarkupContainer parent) {
            super(id);
            this.content = content;
            this.parent = parent;
        }

        public void collapse(AjaxRequestTarget target) {
            if (content.isVisible()) {
                content.setVisible(false);
                target.add(parent);
            }
        }

    }
}
