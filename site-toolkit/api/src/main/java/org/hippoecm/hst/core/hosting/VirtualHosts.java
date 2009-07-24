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
package org.hippoecm.hst.core.hosting;

import org.hippoecm.hst.core.request.MatchedMapping;


/**
 * The container interface for {@link VirtualHost}
 * 
 */
public interface VirtualHosts {

    /**
     * Typically, some paths we do not want to be handle by the hst framework request processing. Typically, this would
     * be for example paths starting with /binaries/, or paths ending with some extension, like .pdf
     * 
     * When a path must be excluded, this method return true.
     * 
     * @param pathInfo
     * @return true when the path must be excluded for matching to a host. 
     */
    boolean isExcluded(String pathInfo);
    
    /**
     * 
     * @param hostName
     * @param pathInfo
     * @return the <code>MatchedMapping</code> or <code>null</code>
     */
    MatchedMapping findMapping(String hostName,String pathInfo);
  
    /**
     * 
     * @return the hostname that is configured as default, or <code>null</code> if none is configured as default.
     */
    String getDefaultHostName();
}
