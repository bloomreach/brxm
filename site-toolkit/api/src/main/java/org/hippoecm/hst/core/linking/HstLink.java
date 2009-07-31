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
package org.hippoecm.hst.core.linking;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

/**
 * HstLink is the object representing a link. The {@link #getPath()} return you the value of the link, and {@link #getPathElements()}
 * returns you the path splitted on "/"'s. The String[] version is more practical because the {@link javax.servlet.http.HttpServletResponse#encodeURL(String)}
 * also encodes slashes. 
 * 
 * Furthermore, the {@link HstSite} that the link is meant for is accessible through this HstLink, because it is needed if the link is
 * out of the scope of the current HstSite. The HstSite can access the {@link org.hippoecm.hst.core.hosting.VirtualHost} through which
 * in turn even links to different hosts can be created. 
 *
 */
public interface HstLink {

    /**
     * Note: This is *not* a url!
     * @return the path of this HstLink. Note: This is *not* a url!
     */
    String getPath();
    
    /**
     * (re)-sets the path of the HstLink
     * @param path
     */
    void setPath(String path);
    
    /**
     * @return <code>true</code> when the HstLink represents a container resource, like a repository binary
     */
    boolean getContainerResource();
    
    /**
     * @param sets the containerResource
     */
    void setContainerResource(boolean containerResource);
    
    /**
     * @param request
     * @param external if true, the returned url is external, in other words including http/https etc
     * @return the url form of this HstLink, which is a url
     */
    String toUrlForm(HstRequest request, HstResponse response, boolean external);
    
    /**
     * @return the path elements of this HstLink, which is the {@link #getPath()} splitted on slashes
     */
    String[] getPathElements();
    
    /**
     * @return the HstSite that can represent this link. This might be an HstSite which is a different one then the
     * HstSite the link was created in. This could result in a cross-domain (different hostname) link being created, depending
     * on the backing {@link org.hippoecm.hst.core.hosting.VirtualHost}
     */
    HstSite getHstSite();
    
}
