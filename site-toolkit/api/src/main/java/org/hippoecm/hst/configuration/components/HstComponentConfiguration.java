package org.hippoecm.hst.configuration.components;

import java.util.Map;
import java.util.SortedMap;

public interface HstComponentConfiguration {

    String getId();

    String getReferenceName();

    String getRenderPath();

    String getContextRelativePath();

    String getComponentContentBasePath();;

    String getComponentClassName();

    SortedMap<String, HstComponentConfiguration> getChildren();

    Map<String, Object> getProperties();

}
