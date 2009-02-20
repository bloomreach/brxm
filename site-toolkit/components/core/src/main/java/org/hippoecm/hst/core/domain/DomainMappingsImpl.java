/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
