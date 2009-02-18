package org.hippoecm.hst.configuration;

import java.util.List;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public interface LocationMapTreeItem {

    List<HstSiteMapItem> getHstSiteMapItems();
    LocationMapTreeItem getChild(String name);
    
}
