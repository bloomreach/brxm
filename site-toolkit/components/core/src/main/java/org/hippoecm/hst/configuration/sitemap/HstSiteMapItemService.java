package org.hippoecm.hst.configuration.sitemap;


import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMapItemService extends AbstractJCRService implements HstSiteMapItem, Service{

    private static final Logger log = LoggerFactory.getLogger(HstSiteMapItem.class);
    
    private HstComponentConfiguration componentService;
    private HstSiteMapItem parentSiteMapItemService;
    private Map<String, HstSiteMapItemService> childSiteMapItemServices;
    private String dataSource;
    private String urlPartName;
    private String url;
    private String componentLocation;
    private boolean repositoryBased;
   
    public HstSiteMapItemService(Node jcrNode, HstSiteMapItem parentSiteMapItemService) {
        super(jcrNode);
        childSiteMapItemServices = new HashMap<String, HstSiteMapItemService>();
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
        return this.childSiteMapItemServices.values().toArray(new HstSiteMapItemService[childSiteMapItemServices.size()]);
    }

    
    public String getComponentLocation() {
        return componentLocation;
    }

    public HstComponentConfiguration getComponentService() {
        return this.componentService;
    }

    public String getDataSource() {
        return this.dataSource;
    }

    public HstSiteMapItem getParent() {
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

    public void setComponentService(HstComponentConfiguration componentService) {
        this.componentService = componentService;
    }
   
    public HstSiteMapItem[] getChilds() {
        return this.childSiteMapItemServices.values().toArray(new HstSiteMapItem[childSiteMapItemServices.size()]);
    }

    public void addChild(HstSiteMapItemService siteMapService) {
        this.childSiteMapItemServices.put(siteMapService.getUrlPartName(), siteMapService);
    }

    public HstSiteMapItem getChild(String urlPartName) {
        return this.childSiteMapItemServices.get(urlPartName);
    }

    public HstSiteMapItem getChild(HstSiteMapItemMatcher siteMapItemMatcher) {
        HstSiteMapItem[] allChilds =  getChilds();
        for(HstSiteMapItem child : allChilds) {
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
