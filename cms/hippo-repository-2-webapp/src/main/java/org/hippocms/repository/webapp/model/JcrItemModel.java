package org.hippocms.repository.webapp.model;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippocms.repository.webapp.Main;

public class JcrItemModel extends LoadableDetachableModel {
    private static final long serialVersionUID = 1L;

    private String path;

    // constructors

    public JcrItemModel() {
        this.path = "/";
    }

    public JcrItemModel(String path) {
        this.path = path;
    }

    public JcrItemModel(Item item) {
        super(item);
        try {
            this.path = item.getPath();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    // LoadableDetachableModel

    protected Object load() {
        Item result = null;
        try {
            result = (Item) Main.getSession().getItem(path);
            //System.out.println("load: " + path);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

}
