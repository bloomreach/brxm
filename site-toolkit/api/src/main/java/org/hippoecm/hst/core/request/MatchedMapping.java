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
package org.hippoecm.hst.core.request;

import org.hippoecm.hst.core.hosting.Mapping;

public interface MatchedMapping {

    /**
     * If an instance of this MatchedMapping is created with a Mapping, this <code>Mapping</code> is returned.
     * If the instance was created without an actual backing <code>Mapping</code>, which is the case when there are 
     * no virtual hosts configured and the site is using the default siteName, this method may return <code>null</code> for
     * the Mapping
     * 
     * @return the <code>Mapping</code> this MatchedMapping is created with or <code>null</code>
     */
    Mapping getMapping();

    boolean isURIMapped();

    String getSiteName();
    
    String mapToInternalURI(String pathInfo);

}
