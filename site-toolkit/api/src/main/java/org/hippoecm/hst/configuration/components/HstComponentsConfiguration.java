package org.hippoecm.hst.configuration.components;

import java.util.Map;

public interface HstComponentsConfiguration {

    Map<String, HstComponentConfiguration> getComponentConfigurations();

    HstComponentConfiguration getComponentConfiguration(String id);

}
