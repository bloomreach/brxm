/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.component;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;


/**
 * HstURL Factory interface.
 * It is mainly responsible to generate HstURL.
 * 
 * @version $Id$
 */
public interface HstURLFactory {
    

    /**
     * Returns the HstContainerURLProvider.
     * 
     * @return HstContainerURLProvider
     */
    HstContainerURLProvider getContainerURLProvider();

    /**
     * Returns HstURL for the HstURL type with reference namespace based on the base container URL
     * 
     * @param type the HstURL type. It should one of {@link HstURL#ACTION_TYPE}, {@link HstURL#RENDER_TYPE} or {@link HstURL#RESOURCE_TYPE}.
     * @param referenceNamespace the reference namespace of the HstComponent's window.
     * @param base the base HstContainer URL
     * @param requestContext the current HstRequestContext
     * @return HstContainerURLProvider
     */
    HstURL createURL(String type, String referenceNamespace, HstContainerURL base, HstRequestContext requestContext);
    
    /**
     * Returns HstURL for the HstURL type with reference namespace based on the base container URL and an explicit
     * <code>contextPath</code>, for example needed when the {@link Mount} to create a link for has a different contextpath 
     * than the {@link ResolvedMount} belonging to the {@link HstRequestContext}
     * 
     * @param type the HstURL type. It should one of {@link HstURL#ACTION_TYPE}, {@link HstURL#RENDER_TYPE} or {@link HstURL#RESOURCE_TYPE}.
     * @param referenceNamespace the reference namespace of the HstComponent's window.
     * @param base the base HstContainer URL
     * @param requestContext the current HstRequestContext
     * @param contextPath the context path for the URL to create. If it is <code>null</code> the 
     * contextPath from the {@link ResolvedMount} will be used. If is is EMPTY string, the contextPath will be set to empty ""
     * @return HstContainerURLProvider
     */
    HstURL createURL(String type, String referenceNamespace, HstContainerURL base, HstRequestContext requestContext, String contextPath);
    
    /**
     * Returns the flag if parameter namespacing is ignored or not. It returns false by default.
     * @return
     */
    boolean isReferenceNamespaceIgnored();
    
}
