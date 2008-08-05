package org.hippoecm.hst.core.template;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			Map moduleMap = (Map) request.getSession().getAttribute(Module.REQUEST_MODULEMAP_ATTRIBUTE);
			if (moduleMap != null) {
			    Iterator moduleKeyIterator = moduleMap.keySet().iterator();
			    while (moduleKeyIterator.hasNext()) {
			    	String moduleName = (String) moduleKeyIterator.next();
			    	String moduleClassName = (String) moduleMap.get(moduleName);
					if (moduleName != null) {
						log.info("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ" + moduleName);
						try {
							Module module = getModule(moduleClassName);
							
							PageNode pageNode = getPageNode(request);
							ModuleNode moduleNode = pageNode.getModuleNodeByName(moduleName);
							module.setModuleNode(moduleNode);
							forward = module.execute((HttpServletRequest) request, (HttpServletResponse) response);						
						} catch (Exception e) {
							throw new ServletException(e);
						}
						
					}
			    }
			}
			request.getSession().removeAttribute(Module.REQUEST_MODULEMAP_ATTRIBUTE);
			if (forward != null) {
				log.info("FOORRRRWAAARD" + forward);
				chain.doFilter(new RequestWrapper((HttpServletRequest) request, forward), response);
			} else {
			    chain.doFilter(request, response);
			}
		}
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
