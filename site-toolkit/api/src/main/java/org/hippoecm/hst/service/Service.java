package org.hippoecm.hst.service;

import org.hippoecm.hst.provider.ValueProvider;

public interface Service {
    
    /**
     * @return ValueProvider giving access to the underlying object providing the values
     */
    public ValueProvider getValueProvider();
    
    /**
     * 
     * @return an array of child Services. If there are no child services, an empty array is returned
     */
    public Service[] getChildServices();
    
    public void closeValueProvider(boolean closeChildServices);
    
}
