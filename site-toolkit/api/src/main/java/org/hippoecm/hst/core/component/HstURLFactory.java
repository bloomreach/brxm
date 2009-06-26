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
package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.request.HstRequestContext;


/**
 * HstURL Factory interface.
 * It is mainly responsible to generate HstURL.
 * 
 * @version $Id$
 */
public interface HstURLFactory {
    
    /**
     * Returns HstURLProvider implementation for servlet environment.
     * 
     * @return HstContainerURLProvider
     */
    HstContainerURLProvider getServletUrlProvider();
    
    /**
     * Returns HstURLProvider implementation for portlet environment.
     * 
     * @return HstContainerURLProvider
     */
    HstContainerURLProvider getPortletUrlProvider();

    /**
     * Returns HstURL for the HstURL type with reference namespace based on the base container URL
     * 
     * @param type the HstURL type. It should one of {@link HstURL#ACTION_TYPE}, {@link HstURL#RENDER_TYPE} or {@link HstURL#RESOURCE_TYPE}.
     * @param referenceNamespace the reference namespace of the HstComponent's window.
     * @param base the base HstContainer URL
     * @return HstContainerURLProvider
     */
    HstURL createURL(String type, String referenceNamespace, HstContainerURL base);
    
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
     * Returns the flag if parameter namespacing is ignored or not. It returns false by default.
     * @return
     */
    boolean isReferenceNamespaceIgnored();
    
}
