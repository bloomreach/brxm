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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.node.ModuleNode;

/** 
 * @deprecated
 *
 */
public class ModuleTag extends TagSupport {
	
	private String className;
	private String contextBaseRequestParameter;

	@Override
	public int doEndTag() throws JspException {
		
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();	
		HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
		ModuleNode moduleNode = (ModuleNode) request.getAttribute("currentModuleNode");
		
		Module module;
		try {
			module = getModule();
		} catch (Exception e) {
			throw new JspException(e);
		}
		//module.setContextBase(contextBase);
/*		try {
			module.setModuleNode(moduleNode);
			module.execute(request, response);
		} catch (TemplateException e) {
			throw new JspException(e);
		}
*/
		return super.doEndTag();
	}
	
	private Module getModule() throws Exception {
		Object o = null;
			o = Class.forName(getClassName()).newInstance();
			if (!Module.class.isInstance(o)) {
				throw new Exception(getClassName() + " does not implement the interface " + Module.class.getName());
			}
		return (Module) o;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	
	

}
