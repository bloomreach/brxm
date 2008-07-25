package org.hippoecm.frontend.plugins.standards.list.resolvers;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;

public interface IListAttributeModifier extends IClusterable {
    
    public AttributeModifier getCellAttributeModifier(IModel model);
    
    public AttributeModifier getColumnAttributeModifier(IModel model);
}
