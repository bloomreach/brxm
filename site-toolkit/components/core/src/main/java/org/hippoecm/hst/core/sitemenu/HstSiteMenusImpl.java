package org.hippoecm.hst.core.sitemenu;

import java.util.Map;

import javax.jcr.Session;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

public class HstSiteMenusImpl implements HstSiteMenus{

    private static final long serialVersionUID = 1L;

    public HstSiteMenusImpl(ResolvedSiteMapItem resolvedSiteMapItem, Session session) {
        
    }

    public HstSiteMenu getSelectedSiteMenu() {
        return null;
    }

    public HstSiteMenu getSiteMenu(String name) {
        return null;
    }

    public Map<String, HstSiteMenu> getSiteMenus() {
        return null;
    }

}
