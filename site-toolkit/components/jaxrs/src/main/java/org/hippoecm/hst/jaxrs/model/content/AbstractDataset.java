package org.hippoecm.hst.jaxrs.model.content;

import java.io.Serializable;

public abstract class AbstractDataset implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long totalSize = -1;
    
    private long beginIndex = -1;
    
    public AbstractDataset() {
        
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
