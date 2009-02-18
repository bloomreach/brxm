package org.hippoecm.hst.core.domain;

import java.util.List;

public class DomainMappingsImpl implements DomainMappings {
    
    protected List<DomainMapping> domainMappings;
    protected String defaultSiteName;

    public DomainMappingsImpl(List<DomainMapping> domainMappings) {
        this.domainMappings = domainMappings;
    }
    
    public void setDefaultSiteName(String defaultSiteName) {
        this.defaultSiteName = defaultSiteName;
    }
    
    public List<DomainMapping> getDomainMappings() {
        return this.domainMappings;
    }

    public DomainMapping findDomainMapping(String domainName) {
        DomainMapping domainMapping = null;
        
        for (DomainMapping mapping : this.domainMappings) {
            if (mapping.getDomainName().equals(domainName)) {
                domainMapping = mapping;
                break;
            }
        }
        
        if (domainMapping == null && this.defaultSiteName != null) {
            domainMapping = new DomainMappingImpl(domainName, this.defaultSiteName);
        }
        
        return domainMapping;
    }

}
