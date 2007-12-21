/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.template;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.IClusterable;

public class FieldDescriptor implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String path;
    private String template;
    private String renderer;

    private boolean multiple;
    private boolean binary;
    private boolean prot;
    private boolean mandatory;
    private boolean node;

    public FieldDescriptor(PropertyDefinition pd) {
        name = pd.getName();
        path = pd.getName();
        template = null;
        renderer = null;

        multiple = pd.isMultiple();
        prot = pd.isProtected();
        binary = pd.getRequiredType() == PropertyType.BINARY;
        mandatory = pd.isMandatory();
        node = false;
    }

    public FieldDescriptor(NodeDefinition nd) {
        name = nd.getName();
        path = nd.getName();
        if(nd.getDefaultPrimaryType() != null)
            template = nd.getDefaultPrimaryType().getName();
        else
            template = "";
        renderer = null;
        
        multiple = nd.allowsSameNameSiblings();
        prot = nd.isProtected();
        binary = false;
        mandatory = nd.isMandatory();
        node = true;
    }
    
    public FieldDescriptor(String name, String path, String template, String renderer) {
        this.name = name;
        this.path = path;
        this.template = template;
        this.renderer = renderer;
        
        multiple = prot = binary = mandatory = false;
    }

    public String getName() {
        return name;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getTemplate() {
        return template;
    }
    
    public String getRenderer() {
        return renderer;
    }
    
    public boolean isMultiple() {
        return multiple;
    }
    
    public boolean isBinary() {
        return binary;
    }
    
    public boolean isProtected() {
        return prot;
    }
    
    public boolean isMandatory() {
        return mandatory;
    }
    
    public boolean isNode() {
        return node;
    }

    public boolean isLarge() {
        return false;
    }
}
