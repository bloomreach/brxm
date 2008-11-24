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
		
		String url = getUrl( pageContext,  urlMapping, ctxBase);
		
		if(url!=null) {
			SimpleRssReader reader = new SimpleRssReader();
			feed = reader.read(url);
		} else {
			RssFeed.log.warn("Cannot getch rss feed because url is null");
		}
		pageContext.setAttribute(getVar(), feed);   
		
	}

	public String getUrl(PageContext pageContext, URLMapping urlMapping,
			ContextBase ctxBase){
		String url = null;
		if(this.moduleParameters == null || this.moduleParameters.containsKey("url")) {
			url = moduleParameters.get("url");
		} else {
			RssFeed.log.warn("There is no module parameter specifying the rss url (parameter name = 'url'). Return null");
		}
		return url;
	}

}
