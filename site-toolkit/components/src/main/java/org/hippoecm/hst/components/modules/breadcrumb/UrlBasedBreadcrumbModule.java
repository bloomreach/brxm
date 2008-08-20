package org.hippoecm.hst.components.modules.breadcrumb;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.components.modules.navigation.RepositoryBasedNavigationModule;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.ContextBaseFilter;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlBasedBreadcrumbModule extends RepositoryBasedNavigationModule {

	private static final Logger log = LoggerFactory.getLogger(UrlBasedBreadcrumbModule.class);

	public void render(PageContext pageContext) {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);
        String urlPrefix = (String) request.getAttribute(ContextBaseFilter.URLBASE_INIT_PARAMETER);
        String originalRequest = (String) request.getAttribute(ContextBaseFilter.ORIGINAL_REQUEST_URL);
        String selectedLocation= originalRequest.substring(urlPrefix.length());
    	
    	ArrayList<String> selectedItemsList = new ArrayList<String>();
    	if(selectedLocation!=null){
    	  String [] selectedItems = selectedLocation.split("/");	    	  
    	  for(int i=0;i<selectedItems.length;i++){
    		  if(selectedItems[i].length()>0){
                selectedItemsList.add(selectedItems[i]);
    		  }
    	  }
    	}    	

		pageContext.setAttribute(getVar(), selectedItemsList);

	}

}