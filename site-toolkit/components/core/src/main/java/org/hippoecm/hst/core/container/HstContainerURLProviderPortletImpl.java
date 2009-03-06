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
package org.hippoecm.hst.core.container;

import java.io.UnsupportedEncodingException;

import javax.portlet.BaseURL;
import javax.portlet.MimeResponse;

import org.hippoecm.hst.container.HstContainerPortlet;
import org.hippoecm.hst.container.HstContainerPortletContext;

public class HstContainerURLProviderPortletImpl extends AbstractHstContainerURLProvider {
    
    @Override
    public String toURLString(HstContainerURL containerURL) throws UnsupportedEncodingException {
        String path = buildHstURLPath(containerURL);
        
        BaseURL portletURL = null;
        MimeResponse response = (MimeResponse) HstContainerPortletContext.getCurrentResponse();
        
        if (containerURL.getActionWindowReferenceNamespace() != null) {
            portletURL = response.createActionURL();
        } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
            portletURL = response.createResourceURL();
        } else {
            portletURL = response.createRenderURL();
        }
        
        portletURL.setParameter(HstContainerPortlet.HST_URL_PARAM_NAME, path);
        
        return portletURL.toString();
    }
    
}
