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

package org.hippoecm.hst.plugins.frontend.editor.dao;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;
import org.hippoecm.hst.plugins.frontend.util.JcrUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SitemapItemDAO extends EditorDAO<SitemapItem> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(SitemapItemDAO.class);

    private static final String PAGE_PROPERTY = "hst:componentconfigurationid";
    private static final String CONTENT_PATH_PROPERTY = "hst:relativecontentpath";
    private static final String PORTLET_PROPERTY = "hst:portletcomponentconfigurationid";

    public SitemapItemDAO(IPluginContext context, String namespace) {
        super(context, namespace);
    }

    @Override
    public SitemapItem load(JcrNodeModel model) {
        SitemapItem item = new SitemapItem(model);
        HstContext ctx = getHstContext();

        //Set matcher value
        try {
            String nodeName = model.getNode().getName();
            item.setMatcher(ctx.sitemap.decodeMatcher(nodeName));
        } catch (RepositoryException e) {
            log.error("Error setting matcher value", e);
        }

        //set Page value
        if (JcrUtilities.hasProperty(model, PAGE_PROPERTY)) {
            String selectedPage = JcrUtilities.getProperty(model, PAGE_PROPERTY);
            item.setPage(ctx.sitemap.decodePage(selectedPage));
        }

        //set ContentPath value
        if (JcrUtilities.hasProperty(model, CONTENT_PATH_PROPERTY)) {
            item.setContentPath(JcrUtilities.getProperty(model, CONTENT_PATH_PROPERTY));
        }

        if (ctx.site.isPortalConfigEnabled()) {
            if (JcrUtilities.hasProperty(model, PORTLET_PROPERTY)) {
                item.setPortlet(JcrUtilities.getProperty(model, PORTLET_PROPERTY));
            }
        }

        return item;
    }

    @Override
    protected void persist(SitemapItem k, JcrNodeModel model) {
        HstContext ctx = getHstContext();

        //Set matcher value as nodeName
        String newName = ctx.sitemap.encodeMatcher(k.getMatcher());
        k.setModel(JcrUtilities.rename(model, newName));

        //save page
        JcrUtilities.updateProperty(model, PAGE_PROPERTY, ctx.sitemap.encodePage(k.getPage()));

        //save contentPath
        JcrUtilities.updateProperty(model, CONTENT_PATH_PROPERTY, k.getContentPath());

        if (ctx.site.isPortalConfigEnabled()) {
            JcrUtilities.updateProperty(model, PORTLET_PROPERTY, k.getPortlet());
        }
    }

}
