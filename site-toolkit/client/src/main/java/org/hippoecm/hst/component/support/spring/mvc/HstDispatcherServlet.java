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
package org.hippoecm.hst.component.support.spring.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.util.HstRequestUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

public class HstDispatcherServlet extends DispatcherServlet {

    private static final long serialVersionUID = 1L;
    
    protected String modelAndViewOfActionSessionAttributeNamePrefix = HstDispatcherServlet.class.getName() + ".modelAndViewOfAction-";
    
    public void setModelAndViewOfActionSessionAttributeNamePrefix(String modelAndViewOfActionSessionAttributeNamePrefix) {
        this.modelAndViewOfActionSessionAttributeNamePrefix = modelAndViewOfActionSessionAttributeNamePrefix;
    }
    
    public String getModelAndViewOfActionSessionAttributeNamePrefix() {
        return modelAndViewOfActionSessionAttributeNamePrefix;
    }
    
    protected String getModelAndViewOfActionSessionAttributeName(HstRequest hstRequest) {
        return new StringBuilder(getModelAndViewOfActionSessionAttributeNamePrefix()).append(hstRequest != null ? hstRequest.getReferenceNamespace() : "").toString();
    }
    
    @Override
    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);
        
        boolean isActionPhase = (hstRequest != null && HstRequest.ACTION_PHASE.equals(hstRequest.getLifecyclePhase()));
        boolean renderable = true;
        String modelAndViewOfActionSessionAttributeName = getModelAndViewOfActionSessionAttributeName(hstRequest);
        
        if (isActionPhase) {
            View view = mv.getView();
            
            if (view == null || !(view instanceof RedirectView)) {
                renderable = false;
                hstRequest.getSession(true).setAttribute(modelAndViewOfActionSessionAttributeName, mv);
            }
        }
        
        if (renderable) {
            HttpSession session = request.getSession(false);
            ModelAndView modelAndViewOfAction = null;
            
            if (!isActionPhase) {
                modelAndViewOfAction = (session != null ? (ModelAndView) session.getAttribute(modelAndViewOfActionSessionAttributeName) : (ModelAndView) null);
            }
            
            if (modelAndViewOfAction != null) {
                session.removeAttribute(modelAndViewOfActionSessionAttributeName);
                super.render(modelAndViewOfAction, request, response);
            } else {
                super.render(mv, request, response);
            }
        }
    }
    
}
