package org.hippoecm.cmsprototype.frontend.plugins.search;

import org.apache.wicket.model.IModel;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.NodeCell;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.NodeColumn;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.channel.Channel;

public class SearchNodeColumn extends NodeColumn{

    private static final long serialVersionUID = 1L;
    
     public SearchNodeColumn(IModel displayModel, String nodePropertyName ,Channel channel) {
        super(displayModel, nodePropertyName, channel);
    }

    @Override
    protected NodeCell getNodeCell(String componentId, IModel model, String nodePropertyName) {
        return new SearchNodeCell(componentId, (NodeModelWrapper) model, getChannel(), nodePropertyName);
    }


}
