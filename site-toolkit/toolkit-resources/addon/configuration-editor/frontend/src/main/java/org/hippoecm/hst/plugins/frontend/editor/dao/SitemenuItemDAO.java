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
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemenuItem;
import org.hippoecm.hst.plugins.frontend.util.JcrUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SitemenuItemDAO extends EditorDAO<SitemenuItem> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: SitemapItemDAO.java 17644 2009-05-05 12:40:34Z abogaart $";

    static final Logger log = LoggerFactory.getLogger(SitemenuItemDAO.class);

    private static final String SITEMAP_PROPERTY = "hst:referencesitemapitem";

    public SitemenuItemDAO(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public SitemenuItem load(JcrNodeModel model) {
        SitemenuItem item = new SitemenuItem(model);
        HstContext ctx = getHstContext();

        //Set name value
        try {
            String nodeName = model.getNode().getName();
            item.setName(nodeName);
        } catch (RepositoryException e) {
            log.error("Error setting matcher value", e);
        }

        //set referenced sitemap value
        if (JcrUtilities.hasProperty(model, SITEMAP_PROPERTY)) {
            item.setSitemapReference((JcrUtilities.getProperty(model, SITEMAP_PROPERTY)));
        }

        return item;
    }

    @Override
    protected void persist(SitemenuItem k, JcrNodeModel model) {
        HstContext ctx = getHstContext();

        //Set matcher value as nodeName
        String newName = k.getName();
        k.setModel(JcrUtilities.rename(model, newName));

        //save contentPath
        JcrUtilities.updateProperty(model, SITEMAP_PROPERTY, k.getSitemapReference());
    }

}
