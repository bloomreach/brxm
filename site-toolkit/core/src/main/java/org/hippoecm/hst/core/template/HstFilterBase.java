package org.hippoecm.hst.core.template;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HstFilterBase implements Filter {
	public static final String CONTENT_CONTEXT_REQUEST_ATTRIBUTE = "contentContext";
	
	public static final String TEMPLATE_CONFIGURATION_LOCATION = "/hst:configuration/hst:configuration";
	public static final String TEMPLATE_CONTEXTBASE_NAME = "templateContextBase";
	public static final String SITEMAP_RELATIVE_LOCATION = "/hst:sitemap";
	public static final String HSTCONFIGURATION_LOCATION_PARAMETER = "hstConfigurationUrl";
	
	private String hstConfigurationUrl;
	
	private String [] ignoreList = {".gif", ".jpg", ".css", ".js"};
	
	private static final Logger log = LoggerFactory.getLogger(HstFilterBase.class);
	
	public void init(FilterConfig filterConfig) throws ServletException {		
		hstConfigurationUrl = getInitParameter(filterConfig, HSTCONFIGURATION_LOCATION_PARAMETER);
	}
	
	public PageNode getPageNode(HttpServletRequest request, URLMappingTokenizer urlTokenizer, ContextBase templateContextBase) throws TemplateException, RepositoryException {		
		Session session = JCRConnectorWrapper.getTemplateJCRSession(request.getSession());
		
		log.info("URI" + request.getRequestURI());
			//find 
			
			PageNode matchPageNode = null;
            if (urlTokenizer.isMatchingTemplateFound()) {
            	log.info("matching template found");
            	matchPageNode = urlTokenizer.getMatchingPageNode();
            	String documentPath = urlTokenizer.getTemplateMatchRefValue();
            	if (documentPath != null && documentPath.trim().length() > 0) {
            		log.info("setContentNodePath > " + documentPath);
            		matchPageNode.setRelativeContentPath(documentPath);
            	}
            }
            return matchPageNode;
            
	}
	
	protected boolean ignoreMapping(HttpServletRequest request) {
		for (int i=0; i < ignoreList.length; i++) {
		    log.info("compare ignore " + request.getRequestURI() + " with " + ignoreList[i] + " result " + request.getRequestURI().toLowerCase().endsWith(ignoreList[i]));
			if (request.getRequestURI().toLowerCase().endsWith(ignoreList[i])) {
				return true;
			}
		}
		return false;
	}
	
	public PageNode getPageNode(HttpServletRequest request) throws TemplateException, RepositoryException {
		Session session = JCRConnectorWrapper.getTemplateJCRSession(request.getSession());
		ContextBase templateContextBase = new ContextBase(TEMPLATE_CONTEXTBASE_NAME, TEMPLATE_CONFIGURATION_LOCATION, request, session);
		URLMappingTokenizer urlTokenizer = new URLMappingTokenizer(request, getURLMappingNodes(templateContextBase) );
       	return getPageNode(request, urlTokenizer, templateContextBase);
	} 
	
	
	
	protected Map <String, PageNode> getURLMappingNodes(ContextBase templateContextBase) throws RepositoryException {
		Map<String, PageNode> siteMapNodes = new HashMap<String, PageNode>();
		
		
		Node siteMapRootNode = templateContextBase.getRelativeNode(SITEMAP_RELATIVE_LOCATION);
		 
	    NodeIterator subNodes =  siteMapRootNode.getNodes();
	    while  (subNodes.hasNext()) {
	    	Node subNode = (Node) subNodes.next();
	    	log.info("Subnode" + subNode.getName());
	    	if (subNode.isNodeType(HippoNodeType.NT_HANDLE)) {
	    	   log.info("NT_HANDLE");
	    	}
	    	PropertyIterator propIter = subNode.getProperties();
	    	while (propIter.hasNext()) {
	    		Property prop = (Property) propIter.next();
	    		Value propValue = prop.getValue();
	    		log.info("  propertyname " + prop.getName() + " value=" + propValue.getString());
	    		
	    	}
	    	Property urlMappingProperty = subNode.getProperty("hst:urlmapping");
	    	log.info(">>>>>="+ urlMappingProperty.getValue().getString());
	    	siteMapNodes.put(urlMappingProperty.getValue().getString(), new PageNode(templateContextBase, subNode));
	    }
		
		return siteMapNodes;
	}

	protected void verifyInitParameterHasValue(FilterConfig filterConfig, String param) throws ServletException {
	  if (filterConfig.getInitParameter(param) == null) {
          throw new ServletException("Missing init-param " + param);
      }
	}
	
	protected String getInitParameter(FilterConfig filterConfig, String param)
			throws ServletException {
		String parameterValue = filterConfig.getInitParameter(param);
		if (parameterValue == null) {
			throw new ServletException("Missing init-param " + param);
		}
		return parameterValue;
	}
}
