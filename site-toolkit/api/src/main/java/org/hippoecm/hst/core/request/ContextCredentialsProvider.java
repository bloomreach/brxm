/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Credentials;

/**
 * HstRequestContextCredentialsProvider provides credentials based on the provided HstRequestContext.
 * 
 * @version $Id$
 */
public interface ContextCredentialsProvider {
    
    /**
     * Returns the default credentials for this request context if available. Otherwise, returns null.
     * @param requestContext
     */
    Credentials getDefaultCredentials(HstRequestContext requestContext);

    /**
     * Returns the writable credentials for this request context if available. Otherwise, returns null.
     * @param requestContext
     */
    Credentials getWritableCredentials(HstRequestContext requestContext);


    Object getPreviewCredentials(HstRequestContext hstRequestContext);
}
