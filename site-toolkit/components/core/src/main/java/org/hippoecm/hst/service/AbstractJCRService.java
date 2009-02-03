package org.hippoecm.hst.service;

import javax.jcr.Node;

import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;

public abstract class AbstractJCRService implements Service{
    
    private transient JCRValueProvider valueProvider;
    
    public AbstractJCRService(Node jcrNode) {
        valueProvider = new JCRValueProviderImpl(jcrNode);
    }
    
    public JCRValueProvider getValueProvider() {
        return this.valueProvider;
    }
    
    public void closeValueProvider(boolean closeChildServices) {
        if(closeChildServices) {
            for(Service s : getChildServices()) {
                s.closeValueProvider(closeChildServices);
            }
        }
        this.valueProvider.detach();
    }

    public void dump(StringBuffer buf, String indent) {
        buf.append(indent + this.toString());
    }
    
}
