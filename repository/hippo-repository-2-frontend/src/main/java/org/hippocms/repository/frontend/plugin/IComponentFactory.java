package org.hippocms.repository.frontend.plugin;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

public interface IComponentFactory {
    
    public Component getComponent(String id, IModel model);

}
