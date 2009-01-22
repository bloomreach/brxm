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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.module.execution.ExecutionResult;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for moduleTag implementations. Contains various getters and setters to get and set
 * PageModule related classes. Subclasses can override the doExecute() and or the doRender() method
 * to implement the specific behavior of module handling.
 *
 */
public abstract class ModuleTagBase extends BodyTagSupport {
    protected Map <String, String> parameters;
    protected String name;
    protected String className;
    protected String var;
    protected boolean doExecute = false;
    protected boolean doRender = true;
    protected transient ExecutionResult executionResult;
    
    private static final Logger log = LoggerFactory.getLogger(ModuleTagBase.class);
    
	// Really important to clear here used parameters!!
    @Override
	public void release() {
    	cleanup();
		super.release();
	}

    public void cleanup(){
    	if(this.parameters != null) {
    		this.parameters.clear();
    	}
    }
	protected void doExecute(HttpServletRequest request, PageContainerModuleNode pcm) {           
        try {
        	
            Module module = getModule();
            module.setVar(var);
            module.setPageModuleNode(getPageModuleNode(request, pcm.getName()));
            module.setModuleParameters(parameters);
            HstRequestContext hstRequestContext = (HstRequestContext)request.getAttribute(HstRequestContext.class.getName());
            
            log.debug("Executing module '" + className + "' now");
            
            this.executionResult = module.execute(pageContext,hstRequestContext);
            
        } catch (Exception e) {
             log.warn("Error in doExecute : " + e.getMessage());
         }
    }
    
    protected void doRender(HttpServletRequest request, PageContainerModuleNode pcm) throws JspException {           
           try {
               Module module = getModule();
               module.setVar(var);
               module.setPageModuleNode(getPageModuleNode(request, pcm.getName()));
               module.setModuleParameters(parameters);
               HstRequestContext hstRequestContext = (HstRequestContext)request.getAttribute(HstRequestContext.class.getName());
               module.render(pageContext,hstRequestContext, this.executionResult);
            } catch (Exception e) {
                throw new JspException(e);
            }
    }
    
    protected final PageContainerModuleNode getPageModuleNode(HttpServletRequest request, String moduleName)  throws RepositoryException {
        PageContainerNode pcn = (PageContainerNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_CONTAINER_NAME_REQ_ATTRIBUTE);  
        return pcn.getContainerModuleNodeByName(moduleName);
    }
    
    
    protected final void addParameter(String name, String value) {
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        }
        parameters.put(name, value);
    }
    
    protected final String getParameter(String name) {
        if (parameters != null) {
           return parameters.get(name);
        }
        return null;
    }
    
    protected final Module getModule() throws Exception {
        Object o = null;
        o = Class.forName(getClassName()).newInstance();
        if (!Module.class.isInstance(o)) {
            throw new Exception(getClassName() + " does not implement the interface " + Module.class.getName());
        }
        return (Module) o;
    }
    
    
    
    /* std getters & setters */
    
    /* getters & setters */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExecute() {
        return doExecute ? "true" : "false";
    }

    public void setExecute(String execute) {
    	if(execute!=null) {
    		doExecute = "true".equals(execute.toLowerCase().trim());
    	}
    }
    
    public String getVar() {
        return var;
    }

    public void setVar(String varName) {
        this.var = varName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
    
    public void setRender(String render) {
    	if(render != null) {
    		doRender = "true". equals(render.toLowerCase().trim());
    	}
    }
    
    public String getRender() {
        return doRender ? "true" : "false";
    }
    
}

