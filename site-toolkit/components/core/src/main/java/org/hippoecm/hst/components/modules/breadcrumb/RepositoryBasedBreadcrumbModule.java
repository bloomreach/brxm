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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.components.modules.navigation.NavigationItem;
import org.hippoecm.hst.components.modules.navigation.RepositoryBasedNavigationModule;
import org.hippoecm.hst.core.exception.TemplateException;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryBasedBreadcrumbModule extends RepositoryBasedNavigationModule {

    private static final Logger log = LoggerFactory.getLogger(RepositoryBasedBreadcrumbModule.class);

    @Override
    public void render(PageContext pageContext, HstRequestContext hstRequestContext) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        PageNode pn = hstRequestContext.getPageNode();

        String path = null;
        try {
            path = getPropertyValueFromModuleNode(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);
        } catch (TemplateException e) {
            log.error("Cannot get property " + ModuleNode.CONTENTLOCATION_PROPERTY_NAME, e);
        }

        if (path == null) {
            pageContext.setAttribute(getVar(), new ArrayList<NavigationItem>());
            return;
        }
       
        List<NavigationItem> wrappedNodes = new ArrayList<NavigationItem>();
        try {

            String currentLocation = path;
            String selectedLocation = null;
            if (pn.getRelativeContentPath() != null) {
                selectedLocation = pn.getRelativeContentPath().substring(currentLocation.length());
            }
            
            log.debug("Current selected location : {} ",selectedLocation);
            
            if(selectedLocation.startsWith("/")) {
            	selectedLocation = selectedLocation.substring(1);
            }
            
            ArrayList<String> selectedItemsList = new ArrayList<String>();
            if (selectedLocation != null) {
                String[] selectedItems = selectedLocation.split("/");
                for (int i = 0; i < selectedItems.length; i++) {
                    selectedItemsList.add(selectedItems[i]);
                }
            }
            log.debug("Current selected items : {} ",selectedItemsList);
            
            
            Node n = hstRequestContext.getContentContextBase().getRelativeNode(path);
            if(n == null ){
                log.warn("repository path '" + path + "' not found");
                pageContext.setAttribute(getVar(), new ArrayList<NavigationItem>());
                return;
            }
            Node subNode = null;            
            for (int i = 0; i < selectedItemsList.size(); i++) {
                try {                	
                    if (subNode == null) {
                    	log.debug("Trying to fetch node {} from {}",selectedItemsList.get(i).toString(),n.getPath());
                        subNode = n.getNode(selectedItemsList.get(i).toString());                        
                    } else {
                    	log.debug("Trying to fetch node {} from {}",selectedItemsList.get(i).toString(),subNode.getPath());
                        subNode = subNode.getNode(selectedItemsList.get(i).toString());
                    }
                    if (!subNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                        wrappedNodes.add(new NavigationItem(subNode, true));
                    }
                } catch (PathNotFoundException e) {
                    log.warn("PathNotFoundException: " + e.getMessage());
                }
            }

        } catch (RepositoryException e) {
            log.error("RepositoryException: " + e.getMessage());
            wrappedNodes = new ArrayList<NavigationItem>();
        }

        pageContext.setAttribute(getVar(), wrappedNodes);

    }

}