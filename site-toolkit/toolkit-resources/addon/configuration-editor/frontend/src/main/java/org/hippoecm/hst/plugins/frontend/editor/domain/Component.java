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

package org.hippoecm.hst.plugins.frontend.editor.domain;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.model.JcrNodeModel;

public class Component extends EditorBean {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public class Parameter {
        String name;
        String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    boolean reference;
    String referenceName;

    String name;
    String template;
    String componentClassName;
    String serverResourcePath;
    List<Parameter> parameters;

    public Component(JcrNodeModel model) {
        super(model);
        parameters = new ArrayList<Parameter>();
    }

    public boolean isReference() {
        return reference;
    }

    public void setReference(boolean reference) {
        this.reference = reference;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getServerResourcePath() {
        return serverResourcePath;
    }

    public void setServerResourcePath(String serverResourcePath) {
        this.serverResourcePath = serverResourcePath;
    }

    public String getComponentClassName() {
        return componentClassName;
    }

    public void setComponentClassName(String componentClassName) {
        this.componentClassName = componentClassName;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addParameter() {
        parameters.add(new Parameter());
    }

    public void addParameter(String name, String value) {
        Parameter p = new Parameter();
        p.setName(name);
        p.setValue(value);
        parameters.add(p);
    }
    public void removeParameter(int index) {
        parameters.remove(index);
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

}
