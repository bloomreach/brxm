package org.hippoecm.hst.service.jcr;

import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.service.Service;

public interface JCRService extends Service{
    
    /**
     * @return JCRValueProvider giving access to the underlying jcr backed data
     */
    public JCRValueProvider getValueProvider();
    
}
