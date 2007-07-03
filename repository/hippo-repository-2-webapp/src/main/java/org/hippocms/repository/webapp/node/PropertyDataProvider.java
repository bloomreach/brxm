package org.hippocms.repository.webapp.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippocms.repository.webapp.JcrSessionLocator;

public class PropertyDataProvider extends LoadableDetachableModel implements IDataProvider {
    private static final long serialVersionUID = 1L;

    private String path;

    public PropertyDataProvider() {
        this.path = "/";
    }

    public PropertyDataProvider(String path) {
        this.path = path;
    }

    public PropertyDataProvider(Item node) {
        super(node);
        try {
            this.path = node.getPath();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    // Begin LoadableDetachableModel implementation

    protected Object load() {
        Item result = null;
        try {
            result = JcrSessionLocator.getSession().getItem(path);
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    // End LoadableDetachableModel implementation

    // Begin IDataProvider implementation

    public Iterator iterator(int first, int count) {
        PropertyIterator it = getProperties();
        it.skip(first);

        List list = new ArrayList();
        for (int i = 0; i < count; i++) {
            list.add(it.nextProperty());
        }
        return list.iterator();
    }

    public IModel model(Object object) {
        Item item = (Item) object;
        return new PropertyDataProvider(item);
    }

    public int size() {
        return new Long(getProperties().getSize()).intValue();
    }

    public void detach() {
        // TODO Auto-generated method stub
    }

    // End IDataProvider implementation

    private PropertyIterator getProperties() {
        PropertyIterator result = null;
        try {
            Item item = (Item) load();
            if (item.isNode()) {
                Node node = (Node) item;
                result = node.getProperties();
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }
}
