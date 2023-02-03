/*
 *  Copyright 2008-2023 Bloomreach
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
package org.hippoecm.hst.site.request;

import javax.jcr.Credentials;

import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * DefaultContextCredentialsProvider
 * 
 * @version $Id$
 */
public class DefaultContextCredentialsProvider implements ContextCredentialsProvider {
    
    protected Credentials defaultCredentials;
    protected Credentials defaultCredentialsForPreviewMode;
    protected Credentials writableCredentials;
    
    public DefaultContextCredentialsProvider(final Credentials defaultCredentials) {
        this(defaultCredentials, null);
    }
    
    public DefaultContextCredentialsProvider(final Credentials defaultCredentials,
                                             final Credentials defaultCredentialsForPreviewMode) {
        this(defaultCredentials, defaultCredentialsForPreviewMode, null);
    }

    public DefaultContextCredentialsProvider(final Credentials defaultCredentials,
                                             final Credentials defaultCredentialsForPreviewMode,
                                             final Credentials writableCredentials) {
        this.defaultCredentials = defaultCredentials;
        this.defaultCredentialsForPreviewMode = defaultCredentialsForPreviewMode;
        this.writableCredentials = writableCredentials;
    }

    public Credentials getDefaultCredentials(HstRequestContext requestContext) {
        if (defaultCredentialsForPreviewMode != null && requestContext.isPreview()) {
            return defaultCredentialsForPreviewMode;
        }
        
        return defaultCredentials;
    }
    
    public Credentials getWritableCredentials(HstRequestContext requestContext) {
        return writableCredentials;
    }

}
