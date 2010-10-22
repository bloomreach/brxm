package org.hippoecm.hst.jaxrs.model.content;

import java.util.List;

public abstract class AbstractNodeLinkDataset extends AbstractDataset {
    
    private static final long serialVersionUID = 1L;
    
    private List<? extends Link> nodeLinks;
    
    public AbstractNodeLinkDataset() {
        
    }
    
    protected List<? extends Link> getNodeLinks() {
        return nodeLinks;
    }
    
    public void setNodeLinks(List<? extends Link> nodeLinks) {
        this.nodeLinks = nodeLinks;
    }
}
