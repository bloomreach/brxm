package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.configuration.components.HstComponent;
import org.hippoecm.hst.core.mapping.UrlUtilities;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRMatchingSiteMapItemService extends AbstractJCRService implements HstMatchingSiteMapItem{

    private static final Logger log = LoggerFactory.getLogger(HstMatchingSiteMapItem.class);
    
    private HstSiteMapItem delegatee;
    private String urlResidue;
    
    public JCRMatchingSiteMapItemService(HstSiteMapItem siteMapItemService, String urlResidue) {
        // we do not have a jcr node anymore at this point
        super(null);
        this.delegatee = siteMapItemService;
        this.urlResidue = UrlUtilities.normalizeUrl(urlResidue);
    }

    public String getUrlResidue() {
        return this.urlResidue;
    }

    public HstSiteMapItem getChild(String urlPartName) {
       return delegatee.getChild(urlPartName);
    }

    public String getComponentLocation() {
        return delegatee.getComponentLocation();
    }

    public HstComponent getComponentService() {
        return delegatee.getComponentService();
    }

    public String getDataSource() {
        return delegatee.getDataSource();
    }

    public HstSiteMapItem getParent() {
        return delegatee.getParent();
    }

    public String getUrl() {
        return delegatee.getUrl();
    }

    public String getUrlPartName() {
        return delegatee.getUrlPartName();
    }

    public boolean isRepositoryBased() {
        return delegatee.isRepositoryBased();
    }

    public Service[] getChildServices() {
        return new Service[0];
    }

    public void dump(StringBuffer buf, String indent) {
        delegatee.dump(buf, indent);
        buf.append("\n\t").append(indent).append("- URL Residue: ").append(this.getUrlResidue());
        buf.append("\n\t").append(indent).append(":: Components ::").append("\n");
        if(this.getComponentService() != null) {
            dump(buf, indent+ "\t", this.getComponentService());
        } else {
            buf.append("\t").append(indent).append("No Component for matched sitemap item");
            log.warn("The matched sitemap url does not have a ComponentService");
        }
    }

    private void dump(StringBuffer buf, String indent, Service componentService) {
        componentService.dump(buf, indent);
        for(Service childComponentService : componentService.getChildServices()) {
            buf.append("\n");
            dump(buf, indent+"\t", childComponentService);
        }
        
    }
}
