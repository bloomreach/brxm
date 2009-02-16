package org.hippoecm.hst.configuration.sitemap;

import java.util.List;
import java.util.Map;


public interface HstSiteMapItem {

    String getId(); // optional but needs to be unique within the containing HstSiteMap
    String getValue();
    String getPath(); // returns the full path from the item
    boolean isWildCard();
    String getRelativeContentPath();
    String getComponentConfigurationId();
    List<String> getRoles();  
    Map<String, Object> getProperties();
    List<HstSiteMapItem> getChildren();
    HstSiteMapItem getChild(String value);
    HstSiteMap getHstSiteMap();
}
