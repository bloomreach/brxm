package org.hippoecm.hst.ocm;

import javax.jcr.Session;

/**
 * SimpleObjectConverter which is responsible for basic Object-JCR Node mapping.
 * 
 * @version $Id$
 */
public interface SimpleObjectConverter {

    /**
     * Returns a mapped object for the JCR node indicated by the path.
     * 
     * @param session
     * @param path
     * @return
     */
    Object getObject(Session session, String path);
    
}
