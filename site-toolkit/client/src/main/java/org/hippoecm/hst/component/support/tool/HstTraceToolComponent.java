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
package org.hippoecm.hst.component.support.tool;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;

public class HstTraceToolComponent extends GenericHstComponent {
    
    protected String renderPath = "/WEB-INF/jsp/tracetool.jsp";
    
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletContext, componentConfig);
     
        String param = servletContext.getInitParameter("hst.tracetool.render.path");
        
        if (param != null) {
            this.renderPath = param;
        }
    }

    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        response.setRenderPath(this.renderPath);
    }
    
}
