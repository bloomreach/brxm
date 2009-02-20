package org.hippoecm.hst.pagetypes;

import org.hippoecm.hst.service.ServiceNamespace;

@ServiceNamespace(prefix = "testproject")

public interface PageStyleType {
    
    public String getText();
    
}