package org.hippoecm.hst.ocm;

import javax.jcr.Session;

public interface SimpleObjectConverter {

    Object getObject(Session session, String path);
    
}
