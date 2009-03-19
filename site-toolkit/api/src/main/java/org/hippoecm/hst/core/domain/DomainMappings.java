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

/**
 * The container interface for {@link DomainMapping}
 * 
 * @version $Id$
 */
public interface DomainMappings {

    /**
     * Returns the list of {@link DomainMapping} instances.
     * 
     * @return
     */
    List<DomainMapping> getDomainMappings();
    
    /**
     * Returns the proper @{link DomainMapping} for the domainName.
     * 
     * @param domainName
     * @return
     */
    DomainMapping findDomainMapping(String domainName);
    
}
