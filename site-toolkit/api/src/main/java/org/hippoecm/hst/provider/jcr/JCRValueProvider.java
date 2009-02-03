package org.hippoecm.hst.provider.jcr;

import javax.jcr.Node;

import org.hippoecm.hst.provider.ValueProvider;

public interface JCRValueProvider extends ValueProvider{

    public Node getJcrNode();

    /**
     * Method for detaching the jcr node. After calling this method, the jcr node is not available anymore
     */
    public void detach();
    
    /**
     * Test whether the jcr node is detached or not
     * @return true if the node is detached
     */
    public boolean isDetached();
    
    /** 
     * @param nodeType
     * @return true when the underlying jcr node is of type nodeType
     */
    public boolean isNodeType(String nodeType);
}
