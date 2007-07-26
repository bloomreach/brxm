package org.hippocms.repository.frontend.plugin;

import java.lang.reflect.Constructor;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class GenericComponentFactory implements IComponentFactory {

    private String classname;
    
    public GenericComponentFactory(String classname) {
        this.classname = classname;
    }
    
    public Component getComponent(String id, IModel model) {
        Component component;
        try {
            Class resultClass = Class.forName(classname);
            Class[] formalArgs = new Class[] { String.class, IModel.class };
            Constructor constructor = resultClass.getConstructor(formalArgs);
            Object[] actualArgs = new Object[] { id, model };
            component = (Component) constructor.newInstance(actualArgs);
        } catch (Exception e) {
            component = new Label(id, e.getClass().getName() + ": " + e.getMessage());
        }
        return component;
    }

}
