package org.hippoecm.hst.jaxrs.model.content;

import java.util.List;

public abstract class AbstractNodeRepresentationDataset extends AbstractDataset {
    
    private static final long serialVersionUID = 1L;
    
    private List<? extends NodeRepresentation> nodeRepresentations;
    
    public AbstractNodeRepresentationDataset() {
        
    }
    
    protected List<? extends NodeRepresentation> getNodeRepresentations() {
        return nodeRepresentations;
    }
    
    public void setNodeRepresentations(List<? extends NodeRepresentation> nodeRepresentations) {
        this.nodeRepresentations = nodeRepresentations;
    }
}
