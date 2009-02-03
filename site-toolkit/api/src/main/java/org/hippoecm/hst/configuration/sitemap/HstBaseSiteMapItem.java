package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.configuration.components.HstComponent;
import org.hippoecm.hst.service.Service;

public interface HstBaseSiteMapItem extends Service{

    
    public String getComponentLocation();
    
    /**
     * The repositorty content location this sitemap item uses. If the path does not start with a "/", the path is taken
     * relative to the content context base. Using relative paths is preferred. 
     * 
     * @return String repository path, relative to the content context base or absolute to the jcr root node when it starts with a "/"
     */
    public String getDataSource();
    
    /**
     * Returns the url part a sitemap item matches on. This is normally its nodename. When a property 'hst:urlname' is found, this 
     * value is used instead of the nodename. This enables for example a different language having a language specific url space
     * @return String urlpart for this sitemap item
     */
    public String getUrlPartName();
    
    /**
     * Returns the url of the sitemap item, which is the urlPartName of this item + the url of the parent SiteMapItemService 
     */
    public String getUrl();
    
    /**
     * @return whether this sitemap item is repository based
     */
    public boolean isRepositoryBased();
    
    public HstSiteMapItem getParent();

    public HstComponent getComponentService();
}
