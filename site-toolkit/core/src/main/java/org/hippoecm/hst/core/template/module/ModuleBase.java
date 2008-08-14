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

package org.hippoecm.hst.core.template.module;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;

public abstract class ModuleBase implements Module {
	private PageContainerModuleNode pageContainerModule = null;
	private String var = "result";
	
	/**
	 * Override this method your subclasses to execute business logic. Add execute="true" to the jsp tag.
	 */
    public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {
       return null; 
    }

    /**
     * Override this method your subclasses. 
     */
    public void init(HttpServletRequest request) {        
    }

    /**
     * Override this method your subclasses to render logic. Add render="true" to the jsp tag.
     */
    public void render(PageContext pageContext) throws TemplateException {        
    }
	

	public void setPageModuleNode(PageContainerModuleNode node) {
		this.pageContainerModule = node;
	}

	public void setVar(String name) {
		if(name!=null){
			this.var = name;	
		}
	}
	
	public String getVar() {
	    return var;
	}
	
	public String getPropertyValueFromModuleNode(String propertyName) throws TemplateException {		
		return  pageContainerModule.getPropertyValue(propertyName);			
	}

}
