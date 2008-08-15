package org.hippoecm.hst.core.template.module.paginatedbrowse;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.ContextBaseFilter;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaginateModule extends ModuleBase {
	private static final Logger log = LoggerFactory.getLogger(PaginateModule.class);
	
	private int pageSize = 5;
	private String pageIdRequestAttribute;
    private List items;
	
	
	
	public void setItems(List items) {
		this.items = items;
	}
	
	public void setPagesize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	public void setPageIdRequestAttribute(String pageId) {
		this.pageIdRequestAttribute = pageId;
	}
	
	//public abstract List getItemsOfPage(int pageNo);
	//public abstract int getTotalItems();
	
	public String execute(HttpServletRequest request, HttpServletResponse response)
			throws TemplateException {	
		return null;
	}

	public void render(PageContext pageContext) {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		List items = null;
		int pageId = 0;
		long total = 0;
		try {
			//init values from module & request
			setPageIdRequestAttribute(getPropertyValueFromModuleNode("pageIdParameter"));
			pageId = getPageId(request);
			String url = getURL(request);
			String elementClass = getPropertyValueFromModuleNode("elementClass");
			String folderMap = getPropertyValueFromModuleNode("folder");
			
			NodeIterator nodeIterator = getNodeIterator(request, folderMap);
			total = nodeIterator.getSize();
			items = getItems(nodeIterator, request);
			
		} catch (TemplateException e) {			
			log.error("Error getting items", e);
			items = new ArrayList();
		}
		
		PaginatedListBean paginatedData = new PaginatedListBean();
		paginatedData.setItems(items);
		paginatedData.setItemsInPage(items.size());
		paginatedData.setPageId(pageId);
		paginatedData.setTotal(total);
		pageContext.setAttribute("pageItems", paginatedData);
	}
	
	protected String getURL(HttpServletRequest request) throws TemplateException {
		String urlPrefix = (String) request.getAttribute(ContextBaseFilter.URLBASE_INIT_PARAMETER);
		String url = getPropertyValueFromModuleNode("url");
		return urlPrefix + url;
	}
	
	protected int getPageId(HttpServletRequest request) {		
		int pageId = 0;
		try {
			pageId = Integer.parseInt(request.getParameter(pageIdRequestAttribute));
		} catch (NumberFormatException e) {				
			log.error("Cannot parse requestParameter " + pageIdRequestAttribute + " value=" + request.getParameter(pageIdRequestAttribute));
			pageId = 0;
		}
		return pageId;
	}
	
	protected NodeIterator getNodeIterator(HttpServletRequest request, String folderMap) {
		ContextBase contentContextBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);
		Node folderMapNode = contentContextBase.getRelativeNode(folderMap);
		
		NodeIterator nodeIterator = null;
		try {
			nodeIterator = folderMapNode.getNodes();
		} catch (RepositoryException e) {
			log.error("Cannot get nodeIterator for relative folderMap" + folderMap, e);
		}
		return nodeIterator;
	}
	
	protected List<Node> getItems(NodeIterator nodeIterator, HttpServletRequest request) {	
		List<Node> itemList = new ArrayList();
		int pageId = getPageId(request);		 
		nodeIterator.skip(pageId * pageSize);
		int i = 0;
		while (nodeIterator.hasNext() && i++ < pageSize) {
			itemList.add(nodeIterator.nextNode());			
		}
		return itemList;
	}
	
	

}
