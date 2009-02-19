package org.hippoecm.hst.configuration.components;

import java.util.SortedMap;

public interface HstComponentConfiguration extends HstComponentConfigurationBean {

    String getId();

    String getReferenceName();

    String getComponentClassName();

    String getRenderPath();

    SortedMap<String, HstComponentConfiguration> getChildren();

}
