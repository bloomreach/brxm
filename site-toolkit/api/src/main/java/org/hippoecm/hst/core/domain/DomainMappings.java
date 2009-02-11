package org.hippoecm.hst.core.domain;

import java.util.List;

public interface DomainMappings {

    List<DomainMapping> getDomainMappings();
    
    DomainMapping findDomainMapping(String domainName);
    
}
