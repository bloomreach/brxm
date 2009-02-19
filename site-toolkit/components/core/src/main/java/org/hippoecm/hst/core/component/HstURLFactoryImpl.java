package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;

public class HstURLFactoryImpl implements HstURLFactory {
    
    protected String parameterNameComponentSeparator = ContainerConstants.DEFAULT_PARAMETER_NAME_COMPONENTS_SEPARATOR;

    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public String getParameterNameComponentSeparator() {
        return this.parameterNameComponentSeparator;
    }
    
    public HstURL createURL(String type, String parameterNamespace, HstContainerURL baseContainerURL) {
        HstURLProvider provider = new HstURLProviderImpl(baseContainerURL, parameterNamespace, this.parameterNameComponentSeparator);
        HstURLImpl url = new HstURLImpl(provider);
        url.setType(type);
        return url;
    }

}
