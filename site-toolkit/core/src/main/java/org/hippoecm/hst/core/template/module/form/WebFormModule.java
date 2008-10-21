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
package org.hippoecm.hst.core.template.module.form;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.module.form.el.WebFormBean;



public class WebFormModule extends ModuleBase {	

	public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {		
		
		Enumeration attribEnum = request.getAttributeNames();
		
		Map parameterMap = request.getParameterMap();
		Iterator i = parameterMap.keySet().iterator();
	
		String action = getPropertyValueFromModuleNode("action");
		WebFormBean formBean = new WebFormBean();
		formBean.setAction(action);
		request.setAttribute("webform", formBean);
		
		return null;
	}

}
