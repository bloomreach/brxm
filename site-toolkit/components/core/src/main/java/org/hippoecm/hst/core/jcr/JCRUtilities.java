package org.hippoecm.hst.core.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRUtilities {

    private static final Logger log = LoggerFactory.getLogger(JCRUtilities.class);
    
    public static Node getCanonical(Node n){
        if(n instanceof HippoNode) {
            HippoNode hnode = (HippoNode)n;
            try {
                Node canonical = hnode.getCanonicalNode();
                if(canonical == null) {
                    log.warn("Cannot get canonical node for '{}'. This means there is no phyiscal equivalence of the " +
                    		"virtual node. Return null", n.getPath());
                }
                return canonical;
            } catch (RepositoryException e) {
                log.error("Repository exception while fetching canonical node. Return null" , e);
                return null;
            }
        } 
        return n;
    }
}
