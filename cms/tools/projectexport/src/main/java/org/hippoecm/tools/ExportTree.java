/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.tools;

import java.io.Serializable;
import javax.swing.tree.TreeNode;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.wicket1985.Tree;

class ExportTree extends Tree implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final ResourceReference ICO_PROJECT = new ResourceReference(ExportTree.class, "project.gif");
    private static final ResourceReference ICO_NAMESPACE = new ResourceReference(ExportTree.class, "namespace.gif");
    private static final ResourceReference ICO_CONTENT = new ResourceReference(ExportTree.class, "content.gif");

    private Component[] listeners;
    
    ExportTree(String id, ExportTreeModel model, Component[] listeners) {
        super(id, model);
        this.listeners = listeners;
    }

    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
        super.onNodeLinkClicked(target, node);
        for(Component c : listeners) {
            target.addComponent(c);
        }
    }
    
    @Override
    protected ResourceReference getNodeIcon(TreeNode node) {
        if (getModelObject() instanceof ExportTreeModel) {
            Element element = ((ExportTreeModel) getModelObject()).backingElement(node);
            if (element instanceof Element.ProjectElement) {
                return ICO_PROJECT;
            } else if (element instanceof Element.NamespaceElement) {
                return ICO_NAMESPACE;
            } else if (element instanceof Element.ContentElement) {
                return ICO_CONTENT;
            } else {
                return super.getNodeIcon(node);
            }
        } else {
            return super.getNodeIcon(node);
        }
    }

    @Override
    protected Component newIndentation(MarkupContainer parent, String id, final TreeNode node, final int level) {
        WebMarkupContainer result = new WebMarkupContainer(id) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                Response response = RequestCycle.get().getResponse();
                TreeNode parent = node.getParent();
                CharSequence urls[] = new CharSequence[level];
                if (level > 0) {
                    urls[0] = "indent-blank";
                }
                for (int i = 1; i < level; ++i) {
                    if (isNodeLast(parent)) {
                        urls[i] = "indent-blank";
                    } else {
                        urls[i] = "indent-line";
                    }
                    parent = parent.getParent();
                }
                for (int i = level - 1; i >= 0; i--) {
                    response.write("<span class=\"" + urls[i] + "\"></span>");
                }
            }
        };
        result.setRenderBodyOnly(true);
        return result;
    }

    @Override
    protected MarkupContainer newJunctionImage(MarkupContainer parent, final String id, final TreeNode node) {
        return (MarkupContainer) new WebMarkupContainer(id) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);

                final String cssClassInner;
                if (node.isLeaf() == false) {
                    cssClassInner = isNodeExpanded(node) ? "minus" : "plus";
                } else {
                    cssClassInner = "corner";
                }

                String cssClassOuter = isNodeLast(node) ? "junction-last" : "junction";
                if (node.getParent() == null || node.getParent().getParent() == null) {
                    cssClassOuter = "";
                }

                Response response = RequestCycle.get().getResponse();
                response.write("<span class=\"" + cssClassOuter + "\"><span class=\"" +
                        cssClassInner + "\"></span></span>");
            }
        }.setRenderBodyOnly(true);
    }
    
    @Override
    protected MarkupContainer newContextLink(final MarkupContainer parent, String id, final TreeNode node, MarkupContainer content) {
        if (getModelObject() instanceof ExportTreeModel) {
            ExportTreeModel treeModel = (ExportTreeModel) getModelObject();
            Element element = treeModel.backingElement(node);
            if (element instanceof Element.ProjectElement) {
                if(((Element.ProjectElement)element).projectName.equals("")) {
                    return new NewProjectItem(parent, id, treeModel, node, (Element.ProjectElement)element);
                } else {
                    return new ProjectItem(parent, id, treeModel, node, (Element.ProjectElement)element);
                }
            } else if (element instanceof Element.NamespaceElement) {
                return new NamespaceItem(parent, id, treeModel, node, (Element.NamespaceElement)element);
            } else if (element instanceof Element.ContentElement) {
                if (treeModel.backingTreeNode(node) != null && treeModel.backingElement(treeModel.backingTreeNode(node)) instanceof Element.ProjectElement && ((Element.ProjectElement) treeModel.backingElement(treeModel.backingTreeNode(node))).projectName.equals("")) {
                    return new NewContentItem(parent, id, treeModel, node, (Element.ContentElement) element);
                } else {
                    return new ContentItem(parent, id, treeModel, node, (Element.ContentElement) element);
                }
            } else {
                return new EmptyPanel(id);
            }
        } else {
            return new EmptyPanel(id);
        }
    }

    private boolean isNodeLast(TreeNode node) {
        TreeNode parent = node.getParent();
        if (parent == null) {
            return true;
        } else {
            return parent.getChildAt(parent.getChildCount() - 1).equals(node);
        }
    }
}
