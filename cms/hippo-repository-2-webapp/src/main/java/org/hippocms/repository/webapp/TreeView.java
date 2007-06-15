package org.hippocms.repository.webapp;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Enumeration;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.extensions.markup.html.tree.DefaultAbstractTree.LinkType;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.tree.ITreeStateListener;

public class TreeView extends WebPage implements ITreeStateListener {

    private static final long serialVersionUID = 1L;

    public TreeView() throws MalformedURLException, ClassCastException, RemoteException, NotBoundException,
            LoginException, RepositoryException {

        ClientRepositoryFactory repositoryFactory = new org.apache.jackrabbit.rmi.client.ClientRepositoryFactory();
        Repository repositoryConnection = repositoryFactory.getRepository("rmi://localhost:1099/jackrabbit.repository");
        Session repositorySession = repositoryConnection.login(new SimpleCredentials("username", "password"
                .toCharArray()));

        //Create the root node and add it's children
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(repositorySession.getRootNode());
        addChildren(root);

        //Create the treeModel based on the root node
        TreeModel treeModel = new DefaultTreeModel(root);

        //Create the treeComponent based on the treeModel
        final Tree tree = new Tree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            protected String renderNode(TreeNode node) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                Node jcrNode = (Node) treeNode.getUserObject();
                String result;
                try {
                    result = jcrNode.getName();
                } catch (RepositoryException e) {
                    result = e.getMessage();
                }
                return result;
            }
        };
        //Collapse the treeComponent
        tree.getTreeState().collapseAll();

        //Add behaviour to the treeComponent
        tree.getTreeState().addTreeStateListener(this);

        //Disable ajax links for the time being
        tree.setLinkType(LinkType.REGULAR);

        //Add the treeComponent to the webPage
        add(tree);
    }

    private void addChildren(DefaultMutableTreeNode treeNode) throws RepositoryException {
        Node jcrNode = (Node) treeNode.getUserObject();

        for (NodeIterator iter = jcrNode.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system")) {
                treeNode.add(new DefaultMutableTreeNode(node));
            }
        }
    }

    // Behaviour of the treeComponent

    public void nodeExpanded(TreeNode node) {
        if (node == null) {
            return;
        }
        Enumeration children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            try {
                addChildren(child);
            } catch (RepositoryException e) {
                System.out.println("Exception while expanding node " + e.getMessage());
            }
        }
    }

    public void nodeCollapsed(TreeNode node) {
    }

    public void nodeSelected(TreeNode node) {
        if (node != null) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
            Node jcrNode = (Node) treeNode.getUserObject();

            StringBuffer text = new StringBuffer();
            try {
                for (PropertyIterator iter = jcrNode.getProperties(); iter.hasNext();) {
                    Property prop = iter.nextProperty();

                    text.append(prop.getPath() + " [name=" + prop.getName() + "] = ");
                    if (prop.getDefinition().isMultiple()) {
                        Value[] values = prop.getValues();
                        text.append("[ ");
                        for (int i = 0; i < values.length; i++) {
                            text.append((i > 0 ? ", " : "") + values[i].getString());
                        }
                        text.append(" ]");
                    } else {
                        text.append(prop.getString());
                    }
                }
            } catch (ValueFormatException e) {
                text.append(e.getMessage());
            } catch (IllegalStateException e) {
                text.append(e.getMessage());
            } catch (RepositoryException e) {
                text.append(e.getMessage());
            }
//            System.out.println(text.toString());
//            add(new Label("properties", text.toString()));
        }
    }

    public void nodeUnselected(TreeNode node) {
    }

    public void allNodesExpanded() {
    }

    public void allNodesCollapsed() {
    }

}
