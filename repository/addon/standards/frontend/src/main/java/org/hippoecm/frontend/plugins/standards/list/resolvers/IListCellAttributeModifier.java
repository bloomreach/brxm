package org.hippoecm.frontend.plugins.standards.list.resolvers;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;

public interface IListCellAttributeModifier extends IClusterable {
    
    public AttributeModifier getAttributeModifier(IModel model);
}
