package org.hippoecm.hst.core.component;

public class HstURLFactoryImpl implements HstURLFactory {

    public HstURL createURL(String type, String parameterNamespace, HstURL base) {
        HstURLImpl url = new HstURLImpl(parameterNamespace);
        url.setType(type);
        return url;
    }

}
