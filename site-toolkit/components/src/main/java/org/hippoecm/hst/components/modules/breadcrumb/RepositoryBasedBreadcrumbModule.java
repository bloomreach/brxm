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
import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryBasedBreadcrumbModule extends RepositoryBasedNavigationModule {

    private static final Logger log = LoggerFactory.getLogger(RepositoryBasedBreadcrumbModule.class);

    public void render(PageContext pageContext) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        PageNode pn = (PageNode) pageContext.getRequest().getAttribute(HSTHttpAttributes.CURRENT_PAGE_NODE_REQ_ATTRIBUTE);

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
        ContextBase ctxBase = (ContextBase) request.getAttribute(HSTHttpAttributes.CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE);

        List<NavigationItem> wrappedNodes = new ArrayList<NavigationItem>();
        try {

            String currentLocation = path;
            String selectedLocation = null;
            if (pn.getRelativeContentPath() != null) {
                selectedLocation = pn.getRelativeContentPath().substring(currentLocation.length());
            }

            ArrayList<String> selectedItemsList = new ArrayList<String>();
            if (selectedLocation != null) {
                String[] selectedItems = selectedLocation.split("/");
                for (int i = 0; i < selectedItems.length; i++) {
                    selectedItemsList.add(selectedItems[i]);
                }
            }
            Node n = ctxBase.getRelativeNode(path);
            if(n == null ){
                log.warn("repository path '" + path + "' not found");
                pageContext.setAttribute(getVar(), new ArrayList<NavigationItem>());
                return;
            }
            Node subNode = null;
            for (int i = 0; i < selectedItemsList.size(); i++) {
                try {
                    if (subNode == null) {
                        subNode = n.getNode(selectedItemsList.get(i).toString());
                    } else {
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