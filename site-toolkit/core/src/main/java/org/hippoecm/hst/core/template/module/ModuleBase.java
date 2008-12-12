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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.exception.TemplateException;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.module.execution.ExecutionResult;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;

public abstract class ModuleBase implements Module {
	protected PageContainerModuleNode pageContainerModule = null;
	protected String var;
	protected Map<String, String> moduleParameters;
	
	public final void setPageModuleNode(PageContainerModuleNode node) {
		this.pageContainerModule = node;
	}
	
	public final String getPropertyValueFromModuleNode(String propertyName) throws TemplateException {		
	    return pageContainerModule.getPropertyValue(propertyName);	
	}

	public String getVar() {
		return var;
	}

	public final void setVar(String var) {
		this.var = var;
	}

	public void init(HttpServletRequest request) {
	}

	public final void setModuleParameters(Map<String, String> parameters) {
		moduleParameters = parameters;
	}

    
	public ExecutionResult execute(PageContext pageContext,URLMapping urlMapping, ContextBase ctxBase) throws TemplateException {
		return null;
	}

	// Concrete module implementations can decide to need the executionResult by overriding this method and implementing there own execution result
    public void render(PageContext pageContext, URLMapping urlMapping, ContextBase ctxBase, ExecutionResult executionResult) throws TemplateException{
    	this.render(pageContext, urlMapping, ctxBase);
    }
	
	public  void render(PageContext pageContext, URLMapping urlMapping, ContextBase ctxBase) throws TemplateException {
	// to be implemented by subclasses
	}

}
