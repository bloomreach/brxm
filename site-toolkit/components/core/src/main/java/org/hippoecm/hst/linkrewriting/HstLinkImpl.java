package org.hippoecm.hst.linkrewriting;

import org.hippoecm.hst.configuration.HstSite;

public class HstLinkImpl implements HstLink{

    private String path;
    private HstSite hstSite;
    
    public HstLinkImpl(String path, HstSite hstSite){
        this.path = path;
        this.hstSite = hstSite;
    }
    
    public HstSite getHstSite() {
        return this.hstSite;
    }

    public String getPath() {
        return this.path;
    }

}
