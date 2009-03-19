package org.hippoecm.hst.sitemenu;

import java.io.Serializable;
import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Implementation of this interface is the container of all the <code>{@link SiteMenu}</code>'s that are needed in the frontend.
 * 
 * As the implementation will be available (at least, if configured to be so) on the {@link HstRequestContext}, the Map returned by 
 * {@link #getSiteMenus()} would best be an unmodifiable map, as the client, for instance a {@link HstComponent} instance should not be 
 * able to change the SiteMenus, though, this is up to implementation
 */
public interface SiteMenus extends Serializable{

    /**
     * Recommended is to return an unmodifiable map of the avaiable SiteMenu's
     * @return the available {@link SiteMenu}'s as a (recommended unmodifiable) map in this SiteMenus impl
     */
    Map<String, SiteMenu> getSiteMenus();
    
    /**
     * 
     * @param name the name of the {@link SiteMenu}
     * @return the {@link SiteMenu} having the correct name
     */
    SiteMenu getSiteMenu(String name);
    
}
