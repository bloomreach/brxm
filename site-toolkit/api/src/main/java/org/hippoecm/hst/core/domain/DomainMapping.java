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

/**
 * DomainMapping which holds the mapping between domain (host) name and site name.
 * 
 * @version $Id$
 */
public interface DomainMapping {

    /**
     * Returns the domain name.
     * 
     * @return
     */
    String getDomainName();
    
    /**
     * Returns the site name for the domain.
     * 
     * @return
     */
    String getSiteName();
    
}
