package org.hippoecm.hst.jaxrs.model.content;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="dataset")
public abstract class AbstractNodeRepresentationDataset implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private List<NodeRepresentation> nodeRepresentations;
    
    private long totalSize = -1;
    
    private long beginIndex = -1;
    
    public AbstractNodeRepresentationDataset() {
        
    }
    
    public AbstractNodeRepresentationDataset(List<NodeRepresentation> nodeRepresentations) {
        this.nodeRepresentations = nodeRepresentations;
    }
    
    protected List<NodeRepresentation> getNodeRepresentations() {
        return nodeRepresentations;
    }
    
    protected void setNodeRepresentations(List<NodeRepresentation> nodeRepresentations) {
        this.nodeRepresentations = nodeRepresentations;
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    
    public long getBeginIndex() {
        return beginIndex;
    }
    
    public void setBeginIndex(long beginIndex) {
        this.beginIndex = beginIndex;
    }
}
