package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.container.HstContainerURL;

public class HstURLFactoryImpl implements HstURLFactory {

    public HstURL createURL(String type, String parameterNamespace, HstContainerURL baseContainerURL) {
        HstURLProvider provider = new HstURLProviderImpl(baseContainerURL, parameterNamespace);
        HstURLImpl url = new HstURLImpl(provider);
        url.setType(type);
        return url;
    }

}
