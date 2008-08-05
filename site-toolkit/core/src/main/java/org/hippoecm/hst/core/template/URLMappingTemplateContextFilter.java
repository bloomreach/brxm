package org.hippoecm.hst.core.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.template.node.LayoutNode;
import org.hippoecm.hst.core.template.node.NavigationNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.core.template.node.TemplateNode;
import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class URLMappingTemplateContextFilter extends HstFilterBase implements Filter {
	/* use this if relative paths are used in the configuration
	public static final String TEMPLATE_CONFIGURATION_LOCATION = "/hst:configuration";
	public static final String TEMPLATE_CONTEXTBASE_NAME = "templateContextBase";
	
	public static final String TEMPLATE_CONFIGURATION_LOCATION = "/hst:configuration";
	public static final String TEMPLATE_CONTEXTBASE_NAME = "templateContextBase";
	*/
	
	
	

	public static final String SITEMAP_CONTEXTBASE_NAME = "siteMapContextBase";
	
	//public static final String NAVIGATION_LOCATION = "/hst:configuration/hst:navigation";
	//public static final String CONTENT_LOCATION = "";
	
	public static final String PAGENODE_REQUEST_ATTRIBUTE = "pageNode";
	public static final String JCRSESSION_REQUEST_ATTRIBUTE = "jcrSession";
	public static final String NAVIGATION_REQUEST_ATTRIBUTE = "hstNavigationMapLocation";
	
	public static final String NAVIGATION_CONTEXTBASE_REQUEST_ATTRIBUTE = "navigationContextBase";
	public static final String NAVIGATION_CONTEXTBASE_LOCATION = "/hst:configuration/hst:navigation";
	public static final String NAVIGATION_CONTEXTBASE_NAME = "navigationContext";
	
	
	private static final Logger log = LoggerFactory.getLogger(URLMappingTemplateContextFilter.class);
	
	
	public void init(FilterConfig arg0) throws ServletException {
	}
	  
	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		//Map<String, PageNode> urlMappingPageNodes;
		
		HttpServletRequest request = (HttpServletRequest) req;
		
		if (ignoreMapping(request)) {
			log.info("IGNORE " + request.getRequestURI());
			filterChain.doFilter(request, response);
		} else {
			log.info("NO IGNORE " + request.getRequestURI());
			Session session = JCRConnectorWrapper.getTemplateJCRSession(request.getSession());
			
			log.info("URI" + request.getRequestURI());
			//get mapping		
			try {
				ContextBase templateContextBase = new ContextBase(TEMPLATE_CONTEXTBASE_NAME, TEMPLATE_CONFIGURATION_LOCATION, request, session);
			
				//find 
				URLMappingTokenizer urlTokenizer = new URLMappingTokenizer(request, getURLMappingNodes(templateContextBase) );
	           	PageNode matchPageNode = getPageNode(request, urlTokenizer, templateContextBase);
	           	if (matchPageNode != null) {
	            	
	            	log.info("matchPageNode.getURLMappingValue()=" + matchPageNode.getURLMappingValue());
	            	
	            	String urlPrefix = (String) request.getAttribute(ContextBaseFilter.ATTRIBUTENAME_INIT_PARAMETER);
	            	urlPrefix = (urlPrefix == null) ? "" : urlPrefix;
	            	RequestDispatcher dispatcher = request.getRequestDispatcher(urlPrefix + matchPageNode.getLayoutNode().getTemplatePage());
	            	
	            	HttpServletRequestWrapper wrappedRequest = urlTokenizer.getWrappedRequest(request);
	            	//get navigation contextBase
	            	//ContextBase navigationContextBase = new ContextBase(NAVIGATION_CONTEXTBASE_NAME, NAVIGATION_CONTEXTBASE_LOCATION, request);
	            	
	            	//set attributes
	            	wrappedRequest.setAttribute(PAGENODE_REQUEST_ATTRIBUTE, matchPageNode);
	            	wrappedRequest.setAttribute(JCRSESSION_REQUEST_ATTRIBUTE, session);
	            	//wrappedRequest.setAttribute(NAVIGATION_CONTEXTBASE_REQUEST_ATTRIBUTE, navigationContextBase);
	            	
	    			dispatcher.forward(wrappedRequest, response);
	            } else {
	            	log.info("no matching template found for" + request.getRequestURI() );
	            	//what to do? no matching pattern found... lets continue the filter chain...
	            	filterChain.doFilter(req, response);
	            }
			} catch (Exception e) {		
				e.printStackTrace();
				filterChain.doFilter(req, response);
			}
		}
	}

	
	/**
	 * Finds a template string 
	 * @param request
	 * @return
	 */
	private PageNode getPageNode(HttpServletRequest request, Map<String, PageNode> urlMappingToPageNodeMap) {
		String requestURI = request.getRequestURI();
		log.info("requestURI=" + requestURI);
		
		PageNode templateNode = null;
		Iterator<String> mappingIter = urlMappingToPageNodeMap.keySet().iterator();
		while (mappingIter.hasNext()) {
			String regex = mappingIter.next();
			if (isPattern(regex)) {
			   //the match string is a 'real'pattern			
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(requestURI);
				if (matcher.matches()) {
				   templateNode = urlMappingToPageNodeMap.get(regex);
				   //found a match, but continue loop because there may be
				   //a perfect match (i.e. a regex string that equals the URI)
				}
			} else {
			    //the match string is a normal string 
				if (requestURI.equals(regex)) {
				   //a perfect match, so return this mapping
					return urlMappingToPageNodeMap.get(regex);
				}
			}
		}
		return templateNode;
	}
	
	/**
	 * 
	 * @param request
	 * @return
	 * @throws ServletException
	 */
	/*private ContextBase getContextBase(HttpServletRequest request) throws ServletException {
		
		String currentContextAttributeName = (String) request.getAttribute(HstFilterBase.CURRENT_CONTEXT_ATTRIBUTE);
		if (currentContextAttributeName != null) {
		   ContextBase base = (ContextBase) request.getAttribute(currentContextAttributeName);
		   if (base != null) {
			   return base;
		   }
		}
		try {
			return ContextBase.getDefaultContextBase(request);
		} catch (PathNotFoundException e) {		
			throw new ServletException("No valid ContextBase available", e);			
		} catch (RepositoryException e) {		
			throw new ServletException("No valid ContextBase available", e);			
		}
	}*/
	
	
	/**
	 * Determines if the parameter is a 'real' pattern or just a regular String.
	 * (only checks for + and * for now...)
	 * @param s
	 * @return
	 */
	private boolean isPattern(String s) {
		return s.contains("+") ||
		       s.contains("*");
	}
	
	/*public static Map <String, PageNode> getPageNodes(ContextBase templateContextBase) throws RepositoryException {
		Map<String, PageNode> pageNodes = new HashMap<String, PageNode>();
		Node siteMapRootNode =  templateContextBase.getRelativeNode(SITEMAP_RELATIVE_LOCATION);
		  NodeIterator subNodes =  siteMapRootNode.getNodes();
		    while (subNodes.hasNext()) {
		    	PageNode n = new PageNode(templateContextBase, (Node) subNodes.next());
		    	pageNodes.put(n.getURLMappingValue(), n);
		    	
		    }
		return pageNodes;
	}*/
	
	
	
	private static Node getNodeByAbsolutePath(final Session session, final String path) throws PathNotFoundException, RepositoryException{
		String relPath = path.startsWith("/") ? path.substring(1) : path;
		return session.getRootNode().getNode(relPath);
	}

}
