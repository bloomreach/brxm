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
package org.hippoecm.hst.core.sitemenu;

import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMenuItem implements CommonMenuItem{

    private static final Logger log = LoggerFactory.getLogger(AbstractMenuItem.class);
    private ResolvedSiteMapItemWrapper resolvedSiteMapItem;
    
    protected int depth;
    protected boolean repositoryBased;
    protected Map<String, Object> properties;
    protected HstLink hstLink;
    protected String name;
    protected boolean expanded;
    protected boolean selected;
    
    public int getDepth() {
        return this.depth;
    }

    public boolean isRepositoryBased() {
        return repositoryBased;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public HstLink getHstLink() {
        return hstLink;
    }

    public String getName() {
        return name;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isSelected() {
        return selected;
    }

    public ResolvedSiteMapItem resolveToSiteMapItem(HstRequest request) {
        if(this.resolvedSiteMapItem != null) {
            return resolvedSiteMapItem.resolvedItem;
        }
        if(this.getHstLink() == null || this.getHstLink().getPath() == null || "".equals(this.getHstLink().getPath())) {
            log.warn("Cannot resolve to sitemap item because HstLink is null or empty. Return null"); 
            return null;
        }
        HstRequestContext ctx = request.getRequestContext();
        resolvedSiteMapItem = new ResolvedSiteMapItemWrapper(ctx.getSiteMapMatcher().match(this.getHstLink().getPath(), ctx.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite()));
        return resolvedSiteMapItem.resolvedItem;
    }

   private class ResolvedSiteMapItemWrapper {
        
        private ResolvedSiteMapItem resolvedItem;
        
        ResolvedSiteMapItemWrapper(ResolvedSiteMapItem resolvedItem){
            this.resolvedItem = resolvedItem;
        }
    }
}
