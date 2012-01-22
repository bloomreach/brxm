package org.hippoecm.hst.pagecomposer.jaxrs.services;

import org.hippoecm.hst.core.parameters.Parameter;

public interface DummyInfo {

    @Parameter(name = "foo")
    String getParameterOne();
    
    @Parameter(name = "bar", required = true, defaultValue = "test")
    String getParameterTwo();

}
