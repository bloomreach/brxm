package org.hippoecm.frontend.model.tree;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultTreeModel;

import org.hippoecm.frontend.model.JcrNodeModel;

public class JcrTreeModel extends DefaultTreeModel {
    private static final long serialVersionUID = 1L;

    private Map registry;

    public JcrTreeModel(JcrTreeNode rootModel) {
        super(rootModel);
        rootModel.setTreeModel(this);
        
        registry = new HashMap();
        register(rootModel);
    }

    public void register(JcrTreeNode treeNodeModel) {
        String key = treeNodeModel.getNodeModel().getItemModel().getPath();
        registry.put(key, treeNodeModel);
    }
    
    //TODO: Currently treeNodes are never unregistered.
    //
    //Although an unregister method is easy to implement it
    //is not clear when to call it. Maybe a WeakReference HashMap
    //should be used to keep the registry clean.
    //
    //With the current use cases never unregistering doesn't
    //seem to cause much trouble though.
    
    public JcrTreeNode lookup(JcrNodeModel nodeModel) {
        String key = nodeModel.getItemModel().getPath();
        return (JcrTreeNode)registry.get(key);
    }

}
