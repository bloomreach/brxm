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
package org.hippoecm.hst.core.template.tag;


import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleParameterTag  extends BodyTagSupport {
	private static final long serialVersionUID = 1L;
	Logger log = LoggerFactory.getLogger(ModuleParameterTag.class);
	
	private String name;
	private String value;


public int doEndTag() throws JspException {		
		
		Tag t = findAncestorWithClass(this, ModuleRenderTag.class);
    	if (t == null) {
    	    throw new JspTagException("ParameterTag needs to be inside Module tag");
    	}
    	
    	ModuleRenderTag moduleTag = (ModuleRenderTag) t;
    	log.debug("Adding parameter to ModuleRenderTag '" + getName() + "=" + getValue() + "'.");
        moduleTag.addParameter(name, value);  	
		return EVAL_PAGE;
	}


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


