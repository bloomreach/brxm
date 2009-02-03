package org.hippoecm.hst.configuration.sitemap;


import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.pagemapping.components.Component;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRSiteMapItemService extends AbstractJCRService implements SiteMapItem{

    private static final Logger log = LoggerFactory.getLogger(SiteMapItem.class);
    
    private Component componentService;
    private SiteMapItem parentSiteMapItemService;
    private Map<String, SiteMapItem> childSiteMapItemServices;
    private String dataSource;
    private String urlPartName;
    private String url;
    private String componentLocation;
    private boolean repositoryBased;
   
    public JCRSiteMapItemService(Node jcrNode, SiteMapItem parentSiteMapItemService) {
        super(jcrNode);
        childSiteMapItemServices = new HashMap<String, SiteMapItem>();
        this.parentSiteMapItemService = parentSiteMapItemService;
        
        this.dataSource = this.getValueProvider().getString(Configuration.PROPERTYNAME_DATASOURCE);
        this.componentLocation = this.getValueProvider().getString(Configuration.PROPERTYNAME_COMPONENTLOCATION);
        /*
         * if there is a Configuration.PROPERTYNAME_URLNAME property which is non null, this is used as url part, otherwise, which is the common default
         * the nodes nodename is used 
         */ 
        this.urlPartName = this.getValueProvider().getString(Configuration.PROPERTYNAME_URLNAME) == null ? this.getValueProvider().getName() : this.getValueProvider().getString(Configuration.PROPERTYNAME_URLNAME);
        this.repositoryBased = this.getValueProvider().getBoolean(Configuration.PROPERTYNAME_REPOSITORYBASED);
        
        if(parentSiteMapItemService == null) {
            this.url = null;
        } else {
            if(parentSiteMapItemService.getUrl() == null) {
                this.url = urlPartName;
            } else {
                this.url = parentSiteMapItemService.getUrl() + "/" +urlPartName;
            }
        }
    }

    public Service[] getChildServices() {
        return this.getChilds();
    }

    
    public String getComponentLocation() {
        return componentLocation;
    }

    public Component getComponentService() {
        return this.componentService;
    }

    public String getDataSource() {
        return this.dataSource;
    }

    public SiteMapItem getParent() {
        return this.parentSiteMapItemService;
    }

    public String getUrlPartName() {
        return this.urlPartName;
    }
    
    public String getUrl() {
        return this.url;
    }

    public boolean isRepositoryBased() {
        return this.repositoryBased;
    }

    public void setComponentService(Component componentService) {
        this.componentService = componentService;
    }
   
    public SiteMapItem[] getChilds() {
        return this.childSiteMapItemServices.values().toArray(new SiteMapItem[childSiteMapItemServices.size()]);
    }

    public void addChild(SiteMapItem siteMapService) {
        this.childSiteMapItemServices.put(siteMapService.getUrlPartName(), siteMapService);
    }

    public SiteMapItem getChild(String urlPartName) {
        return this.childSiteMapItemServices.get(urlPartName);
    }

    public SiteMapItem getChild(SiteMapItemMatcher siteMapItemMatcher) {
        SiteMapItem[] allChilds =  getChilds();
        for(SiteMapItem child : allChilds) {
            if(siteMapItemMatcher.matches(child)) {
                // return the first child that matches
                return child;
            }
        }
        log.debug("No matching child found");
        return null;
    }
    
    public void dump(StringBuffer buf, String indent) {
        buf.append("\n").append(indent).append("+").append(this.getUrl());
        buf.append("\n\t").append(indent).append("- Datasource: ").append(this.getDataSource());
        buf.append("\n\t").append(indent).append("- Component location: ").append(this.getComponentLocation());
        buf.append("\n\t").append(indent).append("- Repository based: ").append(this.isRepositoryBased());
    }

}
