package org.hippoecm.hst.pagecomposer.jaxrs.services;

import org.hippoecm.hst.core.parameters.Parameter;

public interface DummyInfo {

    @Parameter(name = "parameterOne")
    String getParameterOne();
    
    @Parameter(name = "parameterTwo", required = true, defaultValue = "test")
    String getParameterTwo();

}
