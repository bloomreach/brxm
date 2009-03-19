package org.hippoecm.hst.ocm;

import javax.jcr.Node;

/**
 * Interface to be implemented by beans that wish to be aware of its originating JCR node.
 * 
 * @version $Id$
 */
public interface NodeAware {

    /**
     * Callback that supplies the originating JCR node.
     * 
     * @param node
     */
    void setNode(Node node);
    
}
