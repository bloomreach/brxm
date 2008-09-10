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

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets the template from the request and the content location if present.
 *
 */
public class URLMappingTokenizer {
	private static final Logger log = LoggerFactory.getLogger(URLMappingTokenizer.class);
	
	private String urlBase;
	private String templatePath;	
	
	private String templateMatchValue;  
	private String templateMatchRefValue; //if the pattern has a back reference this string will get the value
	
	private boolean matchingTemplateFound;
	private PageNode matchingPageNode;
	
	private Map<String, PageNode> mappingToPageNodes; //map with urlPatterns & corresponding PageNodes
	
  
    public URLMappingTokenizer(final HttpServletRequest request, Map<String, PageNode> mappingToPageNodes) {
    	this.mappingToPageNodes = mappingToPageNodes;
    	scan(request);
    }
    
    public HttpServletRequestWrapper getWrappedRequest(final HttpServletRequest request) {
    	return new URLTemplateWrappedRequest(request);
    }
    
    private void scan(final HttpServletRequest request) {
    	//TODO get urlBase 
    	urlBase = "/";
    	
    	//get match for template & documentPath
        matchingTemplateFound = findMatchingTemplate(request);
    }
    
    private boolean findMatchingTemplate(final HttpServletRequest request) {    	
    	String requestURI = request.getRequestURI().replaceFirst(request.getContextPath(), "");
    	boolean patternFound = false;
    			
		Iterator<String> patternIter = mappingToPageNodes.keySet().iterator();
		
		while (patternIter.hasNext() && !patternFound) {
			String pagePattern = patternIter.next();
			log.debug("trying to match " + pagePattern + " with " + requestURI);
			//try to find a mapping that matches the requestURI
			Pattern pattern = Pattern.compile(pagePattern); 
			Matcher parameterMatcher = pattern.matcher(requestURI);
			
			if (parameterMatcher.matches()) {
				log.info("match " + pagePattern + " found " + requestURI );
				patternFound = true;
				matchingPageNode = mappingToPageNodes.get(pagePattern); // get appropriate pageNode
				parameterMatcher.reset();
				while (parameterMatcher.find())
				{    				
					templateMatchValue = parameterMatcher.group(0);  //get match
					if (parameterMatcher.groupCount() > 0) {						
					   templateMatchRefValue = parameterMatcher.group(1); // get back reference value if available
					   log.info("templateMatchRefValue " + templateMatchRefValue);
					}
				}
			}
		}
	  	return patternFound;
    }
    
    // getters

	public String getUrlBase() {
		return urlBase;
	}

	public String getTemplatePath() {
		return templatePath;
	}

	public String getTemplateMatchValue() {
		return templateMatchValue;
	}

	public String getTemplateMatchRefValue() {
		return templateMatchRefValue;
	}

	public boolean isMatchingTemplateFound() {
		return matchingTemplateFound;
	}

	public PageNode getMatchingPageNode() {
		return matchingPageNode;
	}
}



class URLTemplateWrappedRequest extends HttpServletRequestWrapper {
	private static final Logger log = LoggerFactory.getLogger(URLTemplateWrappedRequest.class); 
     public URLTemplateWrappedRequest(HttpServletRequest request) {
		super(request);
	}
     
     
	@Override
	public String getRequestURI() {		
		String requestURI = super.getRequestURI();
		log.info("requestURI=" + requestURI);
		return requestURI;
	}
}
