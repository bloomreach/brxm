package org.hippoecm.frontend.plugins.standards.list.resolvers;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;

public interface IListCellRenderer extends IClusterable {

    public Component getRenderer(String id, IModel model);
    
}
