package org.hippoecm.hst.configuration.components;

import java.util.Map;

public interface HstComponentConfigurationBean {

    String getContextRelativePath();

    String getComponentContentBasePath();

    Map<String, Object> getProperties();

}
