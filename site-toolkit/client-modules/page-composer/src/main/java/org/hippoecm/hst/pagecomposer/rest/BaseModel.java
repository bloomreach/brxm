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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpSession;

public class BaseModel {
    final static String SVN_ID = "$Id$";

    private String id;
    private String name;
    private String path;
    private String parentId;

    private String componentClassName;
    private String template;

    private String type;

    public BaseModel() {
    }

    public BaseModel(Node node, HttpSession http) {

        try {
            path = node.getPath();
            id = IdUtil.getId(path, http);
            String parentPath = path.substring(0, path.lastIndexOf('/'));
            parentId = IdUtil.getId(parentPath, http);

            componentClassName = node.getProperty("hst:componentclassname").getString();
            name = node.getName();
            template = node.getProperty("hst:template").getString();
            type = getTypeValue();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    protected String getTypeValue() {
        return "base";
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

}
