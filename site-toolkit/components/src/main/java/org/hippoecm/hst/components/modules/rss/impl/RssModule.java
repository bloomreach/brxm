package org.hippoecm.hst.components.modules.rss.impl;

import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.components.modules.rss.RssFeed;
import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.exception.TemplateException;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.module.ModuleBase;

public class RssModule extends ModuleBase{

	@Override
	public void render(PageContext pageContext, URLMapping urlMapping,
			ContextBase ctxBase) throws TemplateException {
		
		RssFeed feed = null;
		if(this.moduleParameters == null || this.moduleParameters.containsKey("url")) {
			String url = moduleParameters.get("url");
			SimpleRssReader reader = new SimpleRssReader();
			feed = reader.read(url);
			  
		} else {
			RssFeed.log.warn("There is no module parameter specifying the rss url (parameter name = 'url'). Return null");
		}
		pageContext.setAttribute(getVar(), feed);   
		
	}


}
