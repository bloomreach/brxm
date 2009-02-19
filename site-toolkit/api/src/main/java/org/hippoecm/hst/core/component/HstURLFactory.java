package org.hippoecm.hst.core.component;


public interface HstURLFactory {

    HstURL createURL(String type, String parameterNamespace, HstURL base);
    
}
