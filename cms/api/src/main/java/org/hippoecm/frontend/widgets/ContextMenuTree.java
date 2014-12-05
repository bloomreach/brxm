/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeStateListener;
import org.apache.wicket.extensions.markup.html.tree.LinkType;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ContextMenuTree extends DefaultAbstractTree {
    private static final long serialVersionUID = 1L;
    
    public static final Logger log = LoggerFactory.getLogger(ContextMenuTree.class);

    private boolean dirty;

    public ContextMenuTree(String id, TreeModel model) {
        super(id, model);

        setLinkType(LinkType.AJAX);

        ITreeState treeState = getTreeState();
        treeState.setAllowSelectMultiple(false);
        treeState.collapseAll();
        treeState.expandNode(model.getRoot());
        
        model.addTreeModelListener(new ContextMenuTreeListener());

        treeState.addTreeStateListener(new ITreeStateListener() {
            @Override
            public void allNodesCollapsed() {
                dirty = true;
                collapseAllContextMenus();
            }

            @Override
            public void allNodesExpanded() {
                dirty = true;
                collapseAllContextMenus();
            }

            @Override
            public void nodeCollapsed(final Object node) {
                dirty = true;
                collapseAllContextMenus();
            }

            @Override
            public void nodeExpanded(final Object node) {
                dirty = true;
                collapseAllContextMenus();
            }

            @Override
            public void nodeSelected(final Object node) {
                dirty = true;
                collapseAllContextMenus();
            }

            @Override
            public void nodeUnselected(final Object node) {
                dirty = true;
                collapseAllContextMenus();
            }
        });
    }

    private void collapseAllContextMenus() {
        IContextMenuManager menuManager = findParent(IContextMenuManager.class);
        if (menuManager != null) {
            menuManager.collapseAllContextMenus();
        }
    }

    @Override
    protected ResourceReference getCSS() {
        return null;
    }

    protected Component newMenuIcon(MarkupContainer parent, String id, final TreeNode node) {
        return HippoIcon.fromSprite(id, Icon.DROPDOWN_TINY);
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
                    dirty = true;
                }
            }

            @Override
            public void collapse(final AjaxRequestTarget target) {
                // mouseLeave is never triggered when opening the context menu. Because of this the tree has to be 
                // marked dirty so that the mouse listeners on the current item are reset 
                dirty = true;
                super.collapse(target);
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
    public final void onTargetRespond(final AjaxRequestTarget target) {
        super.onTargetRespond(target);
        onTargetRespond(target, dirty);
        dirty = false;
    }

    protected void onTargetRespond(final AjaxRequestTarget target, final boolean dirty) {
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
    
    public class ContextMenuTreeListener implements TreeModelListener, IClusterable {

        @Override
        public void treeNodesChanged(final TreeModelEvent e) {
            dirty = true;
        }

        @Override
        public void treeNodesInserted(final TreeModelEvent e) {
            dirty = true;
        }

        @Override
        public void treeNodesRemoved(final TreeModelEvent e) {
            dirty = true;
        }

        @Override
        public void treeStructureChanged(final TreeModelEvent e) {
            dirty = true;
        }
    }
}
