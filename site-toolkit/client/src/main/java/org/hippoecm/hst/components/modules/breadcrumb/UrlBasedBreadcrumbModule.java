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
package org.hippoecm.hst.components.modules.breadcrumb;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.components.modules.navigation.RepositoryBasedNavigationModule;
import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated 
public class UrlBasedBreadcrumbModule extends RepositoryBasedNavigationModule {

	private static final Logger log = LoggerFactory.getLogger(UrlBasedBreadcrumbModule.class);

	@Override
	public void render(PageContext pageContext, HstRequestContext hstRequestContext) {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        String urlPrefix = (String) request.getAttribute(HSTHttpAttributes.URI_PREFIX_REQ_ATTRIBUTE);

    	//TODO:Re-enable this once the original requestURI is available from the request.
        
        /*
         * TODO take it from the hstRequestContext.getRepositoryMapping
         */
        String originalRequest = (String) request.getAttribute(HSTHttpAttributes.ORIGINAL_REQUEST_URI_REQ_ATTRIBUTE);
        String selectedLocation= originalRequest.substring(urlPrefix.length());        
    	
    	ArrayList<String> selectedItemsList = new ArrayList<String>();
 
    	
    	if(selectedLocation!=null){
    	  String [] selectedItems = selectedLocation.split("/");	    	  
    	  for(int i=0;i<selectedItems.length;i++){
    		  if(selectedItems[i].length()>0){
                selectedItemsList.add(selectedItems[i]);
    		  }
    	  }
    	} 
    	   	

		pageContext.setAttribute(getVar(), selectedItemsList);

	}

}