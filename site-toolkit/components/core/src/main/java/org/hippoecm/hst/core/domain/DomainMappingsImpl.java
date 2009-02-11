package org.hippoecm.hst.core.domain;

import java.util.List;

public class DomainMappingsImpl implements DomainMappings {
    
    protected List<DomainMapping> domainMappings;

    public DomainMappingsImpl(List<DomainMapping> domainMappings) {
        this.domainMappings = domainMappings;
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
        
        return domainMapping;
    }

}
