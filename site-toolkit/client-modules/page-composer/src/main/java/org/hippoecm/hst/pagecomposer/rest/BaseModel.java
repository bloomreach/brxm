/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.rest;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class BaseModel {
    final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(BaseModel.class);

    private String id;
    private String name;
    private String path;
    private String parentId;

    private String componentClassName;
    private String template;

    private String type;
    private String xtype;

    public BaseModel() {
    }

    public BaseModel(Node node) {
        try {
            id = node.getUUID();
            path = node.getPath();
            type = initType().name();
            xtype = initXtype();

            Node parent = node.getParent();
            parentId = parent != null ? parent.getUUID() : null;

            componentClassName = node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME) ?
                    node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString() : "";
            name = node.getName();
            template = node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_).getString();
        } catch (RepositoryException e) {
            log.error("Error setting up BaseModel", e);
        }
    }

    protected Type initType() {
        return Type.COMPONENT;
    }

    protected String initXtype() {
        return null;
    }

    public String getComponentClassName() {
        return componentClassName;
    }

    public void setComponentClassName(String componentClassName) {
        this.componentClassName = componentClassName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getXtype() {
        return xtype;
    }

    public void setXtype(String xtype) {
        this.xtype = xtype;
    }
}
