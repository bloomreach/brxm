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
package org.hippoecm.hst.component.support.portlet;

import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.hippoecm.hst.container.HstContainerPortletContext;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericPortletPrefsEditor extends GenericHstComponent {
    
    static Logger logger = LoggerFactory.getLogger(GenericPortletPrefsEditor.class);
    
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String hstPortletTitle = request.getParameter("hstPortletTitle");
        String hstServletPath = request.getParameter("hstServletPath");
        String hstPathInfo = request.getParameter("hstPathInfo");
        
        PortletRequest portletRequest = HstContainerPortletContext.getCurrentRequest();
        PortletResponse portletResponse = HstContainerPortletContext.getCurrentResponse();
        
        PortletPreferences prefs = portletRequest.getPreferences();
        
        try {
            if (hstPortletTitle != null) {
                prefs.setValue("hstPortletTitle", hstPortletTitle);
            }
            
            if (hstServletPath != null) {
                prefs.setValue("hstServletPath", hstServletPath);
            }
            
            if (hstPathInfo != null) {
                prefs.setValue("hstPathInfo", hstPathInfo);
            }
            
            prefs.store();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to store preferences.", e);
            } else {
                logger.warn("Failed to store preferences. {}", e.toString());
            }
        }
    }
    
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        Map<String, Object> prefValues = new HashMap<String, Object>();
        PortletRequest portletRequest = HstContainerPortletContext.getCurrentRequest();
        
        PortletPreferences prefs = portletRequest.getPreferences();
        
        try {
            prefValues.put("hstPortletTitle", prefs.getValue("hstPortletTitle", ""));
            prefValues.put("hstServletPath", prefs.getValue("hstServletPath", ""));
            prefValues.put("hstPathInfo", prefs.getValue("hstPathInfo", ""));            
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to retrieve preferences.", e);
            } else {
                logger.warn("Failed to retrieve preferences. {}", e.toString());
            }
        }
        
        request.setAttribute("prefValues", prefValues);
    }
    
}
