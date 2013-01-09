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
package org.hippoecm.hst.component.support.portlet;

import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.StateAwareResponse;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstPortletRequestContext;
import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericPortletPrefsEditor extends GenericHstComponent {
    
    private static final String HST_PREFS_EDITOR_PAGE = "hstPrefsEditorPage";

    private static final String HST_SITE_MENU_ATTR_NAME = "hstSiteMenu";

    private static final String HST_SITE_MENU_NAME_PREF = "hstSiteMenuName";

    private static final String PREF_VALUES_ATTR_NAME = "prefValues";

    private static final String HST_EDIT_PREFERENCES = "hstEditPreferences";
    
    private static final String HST_DEFAULT_EDIT_PREF_NAMES = "hstServletPath,hstPathInfo,hstPortletTitle";

    static Logger logger = LoggerFactory.getLogger(GenericPortletPrefsEditor.class);
    
    protected String defaultSiteMenuName = "main";
    
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        HstPortletRequestContext prc = (HstPortletRequestContext)request.getRequestContext();
        
        PortletPreferences prefs = prc.getPortletRequest().getPreferences();
        
        try {
            boolean updated = false;
            
            String editPreferences = prefs.getValue(HST_EDIT_PREFERENCES, HST_DEFAULT_EDIT_PREF_NAMES);
            String [] editPrefNames = editPreferences.split(",");
            
            for (String editPrefName : editPrefNames) {
                String prefName = editPrefName.trim();
                
                if (!"".equals(prefName)) {
                    String [] prefValues = request.getParameterValues(prefName);
                    
                    if (prefValues != null) {
                        if (prefValues.length == 1) {
                            prefs.setValue(prefName, prefValues[0]);
                        } else {
                            prefs.setValues(prefName, prefValues);
                        }
                    }
                    
                    updated = true;
                }
            }
            
            if (updated) {
                prefs.store();
            }
            
            ((StateAwareResponse) prc.getPortletResponse()).setPortletMode(PortletMode.VIEW);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to store preferences.", e);
            } else {
                logger.warn("Failed to store preferences. {}", e.toString());
            }
        }
    }
    
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        HstPortletRequestContext prc = (HstPortletRequestContext)request.getRequestContext();
        PortletPreferences prefs = prc.getPortletRequest().getPreferences();
        
        try {
            request.setAttribute(PREF_VALUES_ATTR_NAME, prc.getPortletRequest().getPreferences().getMap());
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to retrieve preferences.", e);
            } else {
                logger.warn("Failed to retrieve preferences. {}", e.toString());
            }
        }
        
        String hstSiteMenuName = prefs.getValue(HST_SITE_MENU_NAME_PREF, null);
        
        if (hstSiteMenuName == null) {  
            hstSiteMenuName = getComponentConfiguration().getParameter("hstsitemenuname", request.getRequestContext().getResolvedSiteMapItem());
        }
        
        if (hstSiteMenuName == null) {
            hstSiteMenuName = defaultSiteMenuName;
        }
        
        HstSiteMenu menu = request.getRequestContext().getHstSiteMenus().getSiteMenu(hstSiteMenuName);
        
        if (menu != null) {
            EditableMenu editableMenu = menu.getEditableMenu();
            
            if (editableMenu != null) {
                request.setAttribute(HST_SITE_MENU_ATTR_NAME, editableMenu);
            }
        }
        
        String hstPrefsEditorPage = prefs.getValue(HST_PREFS_EDITOR_PAGE, null);
        
        if (hstPrefsEditorPage != null) {
            response.setRenderPath(hstPrefsEditorPage);
        }
    }
    
}
