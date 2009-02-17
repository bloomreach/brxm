package org.hippoecm.hst.configuration.components;

import java.util.Map;
import java.util.SortedMap;

public interface HstComponentConfiguration {

    String getId();

    String getReferenceName();

    String getContextRelativePath();

    String getComponentContentBasePath();

    String getComponentClassName();

    String getRenderPath();

    SortedMap<String, HstComponentConfiguration> getChildren();

    Map<String, Object> getProperties();

}
