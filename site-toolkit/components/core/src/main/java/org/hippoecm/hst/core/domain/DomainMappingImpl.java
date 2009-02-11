package org.hippoecm.hst.core.domain;

public class DomainMappingImpl implements DomainMapping {
    
    protected String domainName;
    protected String siteName;
    
    public DomainMappingImpl(String domainName, String siteName) {
        this.domainName = domainName;
        this.siteName = siteName;
    }

    public String getDomainName() {
        return this.domainName;
    }
    
    public String getSiteName() {
        return this.siteName;
    }

}
