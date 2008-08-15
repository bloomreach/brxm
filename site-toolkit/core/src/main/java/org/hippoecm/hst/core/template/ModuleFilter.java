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
package org.hippoecm.hst.core.template;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that runs the execute() methods from the modules in the moduleList. 
 * This list is taken from the sessionscope and it is filled by the modules in the
 * @author mmeijnhard
 *
 */
public class ModuleFilter extends HstFilterBase implements Filter {
	private static final Logger log = LoggerFactory.getLogger(ModuleFilter.class);
	
	public void destroy() {		
	}

	public void doFilter(ServletRequest req,ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		
		if (ignoreMapping(request)) {
			chain.doFilter(request, response);
		} else {
			String forward = null; 
			List<ModuleRenderAttributes> moduleList = (List) request.getSession().getAttribute(Module.HTTP_MODULEMAP_ATTRIBUTE);
			if (moduleList != null) {
			    Iterator<ModuleRenderAttributes> moduleRenderIterator = moduleList.iterator();
			    while (moduleRenderIterator.hasNext()) {
			    	ModuleRenderAttributes moduleRenderAttributes = moduleRenderIterator.next();
				
						log.info("Module attributes " + moduleRenderAttributes);

						try {
							Module module = getModule(moduleRenderAttributes.getClassName());
							PageNode pageNode = getPageNode(request, moduleRenderAttributes.getPageName());

							PageContainerNode containerNode = pageNode.getPageContainerNode(moduleRenderAttributes.getContainerName()); 
							if (log.isDebugEnabled() && containerNode != null) {
						    	log.debug("containerNode=" + containerNode.getJcrNode().getPath());
							}
							PageContainerModuleNode pageContainerModuleNode = containerNode.getContainerModuleNodeByName(moduleRenderAttributes.getModuleName());
							
							if (log.isDebugEnabled()) {
								log.debug("pageContainerModuleNode=" + pageContainerModuleNode);
								if (pageContainerModuleNode  != null ) {
									log.debug("pageContainerModuleNode path=" + pageContainerModuleNode.getPath());
								}
							}
							module.setPageModuleNode(pageContainerModuleNode);
							forward = module.execute((HttpServletRequest) request, (HttpServletResponse) response);						
						} catch (Exception e) {
							throw new ServletException(e);
						} 
			    }
			}
			request.getSession().removeAttribute(Module.HTTP_MODULEMAP_ATTRIBUTE);
			if (forward != null) {
				chain.doFilter(new RequestWrapper((HttpServletRequest) request, forward), response);
			} else {
			    chain.doFilter(request, response);
			}
		}
	}
	
	public static ModuleNode getModuleNode(HttpServletRequest request, String moduleName) throws RepositoryException {
		PageNode currentPageNode = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
		ModuleNode moduleNode = currentPageNode.getModuleNodeByName(moduleName);
		return moduleNode;
	}

	public void init(FilterConfig filterConfig) throws ServletException {
	}
	
	private Module getModule(String className) throws Exception {
		Object o = null;
			o = Class.forName(className).newInstance();
			if (!Module.class.isInstance(o)) {
				throw new Exception(className + " does not implement the interface " + Module.class.getName());
			}
		return (Module) o;
	}
	
	
    class RequestWrapper extends HttpServletRequestWrapper {
    	private String forward;
    	public RequestWrapper(HttpServletRequest request, String forward)  {
    		super(request);
    		this.forward = forward;
    	}
		@Override
		public String getRequestURI() {
			return forward;
		}
    }

}
