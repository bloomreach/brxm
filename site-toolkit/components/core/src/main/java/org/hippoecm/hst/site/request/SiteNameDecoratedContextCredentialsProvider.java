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
package org.hippoecm.hst.site.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * SiteNameDecoratedContextCredentialsProvider
 * 
 * @version $Id$
 */
public class SiteNameDecoratedContextCredentialsProvider extends DefaultContextCredentialsProvider {
    
    protected Map<String, Credentials> defaultCredentialsCache = Collections.synchronizedMap(new HashMap<String, Credentials>());
    protected Map<String, Credentials> defaultCredentialsForPreviewModeCache = Collections.synchronizedMap(new HashMap<String, Credentials>());
    protected Map<String, Credentials> writableCredentialsCache = Collections.synchronizedMap(new HashMap<String, Credentials>());
    
    protected String siteNameSeparator = "@";
    
    public SiteNameDecoratedContextCredentialsProvider(Credentials defaultCredentials) {
        super(defaultCredentials);
    }
    
    public SiteNameDecoratedContextCredentialsProvider(Credentials defaultCredentials, Credentials defaultCredentialsForPreviewMode) {
        super(defaultCredentials, defaultCredentialsForPreviewMode);
    }
    
    public SiteNameDecoratedContextCredentialsProvider(Credentials defaultCredentials, Credentials defaultCredentialsForPreviewMode, Credentials writableCredentials) {
        super(defaultCredentials, defaultCredentialsForPreviewMode, writableCredentials);
    }
    
    public void setSiteNameSeparator(String siteNameSeparator) {
        this.siteNameSeparator = siteNameSeparator;
    }
    
    public String getSiteNameSeparator() {
        return siteNameSeparator;
    }
    
    public Credentials getDefaultCredentials(HstRequestContext requestContext) {
        Credentials credentials = super.getDefaultCredentials(requestContext);
        
        if (credentials instanceof SimpleCredentials) {
            String siteName = requestContext.getResolvedSiteMapItem().getResolvedMount().getMount().getHstSite().getName();
            String userID = ((SimpleCredentials) credentials).getUserID();
            String userIDWithSiteName = userID + siteNameSeparator + siteName;
            char [] password = ((SimpleCredentials) credentials).getPassword();
            
            if (requestContext.isPreview()) {
                credentials = defaultCredentialsForPreviewModeCache.get(userIDWithSiteName);
                
                if (credentials == null) {
                    credentials = new SimpleCredentials(userIDWithSiteName, password);
                    defaultCredentialsForPreviewModeCache.put(userIDWithSiteName, credentials);
                }
            } else {
                credentials = defaultCredentialsCache.get(userIDWithSiteName);
                
                if (credentials == null) {
                    credentials = new SimpleCredentials(userIDWithSiteName, password);
                    defaultCredentialsCache.put(userIDWithSiteName, credentials);
                }
            }
        }
        
        return credentials;
    }
    
    public Credentials getWritableCredentials(HstRequestContext requestContext) {
        Credentials credentials = super.getWritableCredentials(requestContext);
        
        if (credentials instanceof SimpleCredentials) {
            String siteName = requestContext.getResolvedSiteMapItem().getResolvedMount().getMount().getHstSite().getName();
            String userID = ((SimpleCredentials) credentials).getUserID();
            String userIDWithSiteName = userID + siteNameSeparator + siteName;
            char [] password = ((SimpleCredentials) credentials).getPassword();
            credentials = writableCredentialsCache.get(userIDWithSiteName);
            
            if (credentials == null) {
                credentials = new SimpleCredentials(userIDWithSiteName, password);
                writableCredentialsCache.put(userIDWithSiteName, credentials);
            }
        }
        
        return credentials;
    }


}
