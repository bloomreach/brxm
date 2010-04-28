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
package org.hippoecm.hst.components;

import javax.servlet.ServletContext;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;

public class GenericResourceServingHstComponent extends BaseHstComponent {
    
    protected String staticResourceServePath = "/staticresource";
    protected String repositoryResourceServePath = "/binaries";
    
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletContext, componentConfig);
        
        String param = servletContext.getInitParameter("staticResourceServePath");
        
        if (param != null) {
            this.staticResourceServePath = param;
        }
        
        param = servletContext.getInitParameter("repositoryResourceServePath");
        
        if (param != null) {
            this.repositoryResourceServePath = param;
        }
    }
    
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        
        super.doBeforeServeResource(request, response);
        
        String resourceId = request.getResourceID();
        
        // This example application assumes the resource is a static file
        // if the resourceId starts with "/".
        // If the resourceId does not start with "/",
        // then the container will dispatch the "serveResourcePath" configured in the configuration.
        // If "serverResourcePath" is not configured, then "renderPath" will be used instead.
        
        if (resourceId != null) {
            if (resourceId.startsWith("static:")) {
                response.setServeResourcePath(this.staticResourceServePath);
            } else if (resourceId.startsWith("repository:")) {
                response.setServeResourcePath(this.repositoryResourceServePath);
            }
        }
    }

}
