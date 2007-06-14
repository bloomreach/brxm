package org.hippocms.repository.webapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.PageParameters;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree.LinkType;
import org.apache.wicket.markup.html.WebPage;

public class TreeView extends WebPage {

    private static final long serialVersionUID = 1L;

    public TreeView(final PageParameters parameters) {
        // create a list with sublists
        List l1 = new ArrayList();
        l1.add("test 1.1");
        l1.add("test 1.2");
        List l2 = new ArrayList();
        l2.add("test 2.1");
        l2.add("test 2.2");
        l2.add("test 2.3");
        List l3 = new ArrayList();
        l3.add("test 3.1");
        l2.add(l3);
        l2.add("test 2.4");
        l1.add(l2);
        l1.add("test 1.3");

        // create a tree
        TreeModel treeModel = convertToTreeModel(l1);
        final Tree tree = new Tree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            protected String renderNode(TreeNode node) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                Object userObject = treeNode.getUserObject();
                return (userObject instanceof List) ? "<subtree>" : String.valueOf(treeNode.getUserObject());
            }
        };

        // disable ajax links in this example
        tree.setLinkType(LinkType.REGULAR);

        add(tree);
    }

    private TreeModel convertToTreeModel(List list) {
        TreeModel model = null;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("<root>");
        add(rootNode, list);
        model = new DefaultTreeModel(rootNode);
        return model;
    }

    private void add(DefaultMutableTreeNode parent, List sub) {
        for (Iterator i = sub.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof List) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(o);
                parent.add(child);
                add(child, (List) o);
            } else {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(o);
                parent.add(child);
            }
        }
    }

}