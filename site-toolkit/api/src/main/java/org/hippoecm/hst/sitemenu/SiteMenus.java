package org.hippoecm.hst.sitemenu;

import java.io.Serializable;
import java.util.Map;

public interface SiteMenus extends Serializable{

    Map<String, SiteMenu> getSiteMenus();
    SiteMenu getSiteMenu(String name);
    
}
