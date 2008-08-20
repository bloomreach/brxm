package org.hippoecm.hst.components.modules.content;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.node.ModuleNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentModule extends ModuleBase {

	private static final Logger log = LoggerFactory.getLogger(ContentModule.class);
	
	public void render(PageContext pageContext) throws TemplateException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

		String path = null;
		try {
			path = getPropertyValueFromModuleNode(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);
		} catch (TemplateException e) {
			log.error("Cannot get property " + ModuleNode.CONTENTLOCATION_PROPERTY_NAME, e);
		}
		if(path == null) {
			pageContext.setAttribute(getVar(),null);
			return;
		}
		ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);

		ContentModuleNode contentModuleNode = null;

		Node node = ctxBase.getRelativeNode(path);
		contentModuleNode = new ContentModuleNode(node);

		pageContext.setAttribute(getVar(), contentModuleNode);
	}
}
