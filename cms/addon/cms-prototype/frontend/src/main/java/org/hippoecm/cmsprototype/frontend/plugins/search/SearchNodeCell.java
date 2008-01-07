package org.hippoecm.cmsprototype.frontend.plugins.search;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.cmsprototype.frontend.plugins.list.NodeCell;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.channel.Channel;

public class SearchNodeCell extends NodeCell {

    private static final long serialVersionUID = 1L;

    public SearchNodeCell(String id, NodeModelWrapper model, Channel channel, String nodePropertyName) {
        super(id, model, channel, nodePropertyName);
    }


    @Override
    protected boolean hasDefaultCustomizedLabels(String nodePropertyName) {
        if(SearchPlugin.REP_EXCERPT.equals(nodePropertyName)) {
            return true;
        }
        if("similar".equals(nodePropertyName)) {
            return true;
        }
        return false; 
    }
    
    @Override
    protected void addDefaultCustomizedLabel(NodeModelWrapper model, String nodePropertyName, AjaxLink link) {
        if(model instanceof SearchDocument) {
            SearchDocument searchDoc = (SearchDocument)model;
            if(SearchPlugin.REP_EXCERPT.equals(nodePropertyName)) {
                link.add(new IncludeHtml("label",searchDoc.getExcerpt()));
                return;
            } else if("similar".equals(nodePropertyName)) {
                link.add(new Label("label", searchDoc.getSimilar()));
                return;
            } 
        }
        // add empty label because we should have a SearchDocument model here
        addEmptyLabel(link);
    }

    
    
}
