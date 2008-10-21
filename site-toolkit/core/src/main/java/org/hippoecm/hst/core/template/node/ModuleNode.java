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
package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleNode extends TemplateNode {

    private Logger log = LoggerFactory.getLogger(ModuleNode.class);

    public static final String TEMPLATE_PROPERTY_NAME = "hst:template";
    public static final String CONTENTLOCATION_PROPERTY_NAME = "hst:contentlocation";

    public PageContainerModuleNode pageContainerModuleNode;

    public ModuleNode(ContextBase contextBase, Node jcrModuleNode) {
        super(contextBase, jcrModuleNode);
    }

    public ModuleNode(ContextBase contextBase, String relativePath) throws RepositoryException {
        super(contextBase, relativePath);
    }

    public String getTemplatePage() throws RepositoryException {
        return jcrNode.getProperty(TEMPLATE_PROPERTY_NAME).getValue().getString();
    }

    public Node getContentLocation() throws RepositoryException {
        return getTemplateNodeFromPropertyValue(CONTENTLOCATION_PROPERTY_NAME);
    }

    public void setPageContainerModuleNode(PageContainerModuleNode pageContainerModuleNode) {
        this.pageContainerModuleNode = pageContainerModuleNode;
    }

    public String getPropertyValue(String propertyName) {
        String containerModuleNodePath = null;
        try {
            if (pageContainerModuleNode != null) {
                Property property;
                containerModuleNodePath = pageContainerModuleNode.getJcrNode().getPath();
                property = pageContainerModuleNode.getJcrNode().getProperty(propertyName);
                if (property != null) {
                    return property.getString();
                } 
                return null;
            }
        } catch (PathNotFoundException e) {
            log.error("PathNotFoundException for pageContainerModuleNode " + containerModuleNodePath + " : " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("RepositoryException: ", e);
        }
        //try to get property from the moduleNode
        try {
			return super.getPropertyValue(propertyName);
		} catch (TemplateException e) {
			log.error("Cannot get property " + propertyName + " from superClass");
			return null;
		}
    }

}
