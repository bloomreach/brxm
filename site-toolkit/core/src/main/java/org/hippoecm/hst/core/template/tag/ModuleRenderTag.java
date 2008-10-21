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
import javax.servlet.jsp.JspException;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The tag class that performs the render() and or execute() methods in a module template (JSP).
 *
 */
public class ModuleRenderTag extends ModuleTagBase {
	
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ModuleRenderTag.class);
	
	public int doStartTag() throws JspException {		
	    return EVAL_BODY_BUFFERED;
	}
	
	public int doEndTag() throws JspException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		PageContainerModuleNode pcm = null;
		if (doExecute || doRender) {
			pcm = (PageContainerModuleNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE);
		}
		if (doExecute) {
			doExecute(request, pcm);
		}
		
		if (doRender) {
		   doRender(request, pcm);
		}
		return EVAL_PAGE;
	}
}


