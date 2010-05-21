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
package org.hippoecm.hst.services.support.jaxrs.content.workflow;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.ClassUtils;
import org.hippoecm.repository.api.Workflow;

/**
 * WorkflowContent
 * 
 * @version $Id$
 */
@XmlRootElement(name = "workflow")
public class WorkflowContent {
    
    private Map<String, String> hints;
    private Collection<String> interfaceNames;
    
    public WorkflowContent() {
    }
    
    public WorkflowContent(Workflow workflow) throws Exception {
        hints = new HashMap<String, String>();
        
        for (Map.Entry<String, Serializable> entry : workflow.hints().entrySet()) {
            hints.put(entry.getKey(), stringifyHintValue(entry.getValue()));
        }
        
        interfaceNames = new LinkedList<String>();
        List<Class> intrfcs = ClassUtils.getAllInterfaces(workflow.getClass());
        
        for (Class intrfc : intrfcs) {
            if (Workflow.class.isAssignableFrom(intrfc)) {
                interfaceNames.add(intrfc.getName());
            }
        }
    }
    
    public Map<String, String> getHints() {
        return hints;
    }
    
    public void setHints(Map<String, String> hints) {
        this.hints = hints;
    }
    
    @XmlElementWrapper(name="interfaces")
    @XmlElement(name="interface")
    public Collection<String> getInterfaceNames() {
        return interfaceNames;
    }
    
    public void setInterfaceNames(Collection<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }
    
    private String stringifyHintValue(Serializable value) {
        if (value != null) {
            return value.toString();
        }
        
        return "";
    }
}
