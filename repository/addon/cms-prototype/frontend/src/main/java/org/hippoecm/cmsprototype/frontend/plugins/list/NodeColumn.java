package org.hippoecm.cmsprototype.frontend.plugins.list;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;

public class NodeColumn extends PropertyColumn {
    private static final long serialVersionUID = 1L;

    public NodeColumn(IModel displayModel, String sortProperty) {
        super(displayModel, sortProperty);
    }


    @Override
    public void populateItem(Item item, String componentId, IModel model)
    {
        item.add(new NodeCell(componentId, (JcrNodeModel) model));
    }

    
    
}
