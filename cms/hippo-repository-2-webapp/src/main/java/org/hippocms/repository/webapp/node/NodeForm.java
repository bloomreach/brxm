package org.hippocms.repository.webapp.node;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;

public class NodeForm extends Form implements INodeEditor {
    private static final long serialVersionUID = 1L;

    private PropertyDataProvider dataProvider;
    
    public NodeForm(String id) {
        super(id);
        
        dataProvider = new PropertyDataProvider();      
        setModel(dataProvider);
        
        PropertiesView propertiesView = new PropertiesView("properties", dataProvider);
        add(propertiesView);
        
        Button saveButton = new Button("save");
        add(saveButton);
    }

    public void setNode(Node node) {
        try {
            //FIXME: temporary hack to work around HREPTWO-28
            String path = node.getPath().substring(1);
            dataProvider.setPath(path);   
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }            
    }

    protected void onSubmit() {
        try {            
            Node node = (Node)getModelObject();
            Iterator it = node.getProperties();
            while (it.hasNext()) {
                Property prop = (Property) it.next();
                prop.save();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }
    
}
