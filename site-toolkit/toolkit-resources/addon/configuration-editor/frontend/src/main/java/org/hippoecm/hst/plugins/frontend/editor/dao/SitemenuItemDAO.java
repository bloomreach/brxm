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
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemenuItem;
import org.hippoecm.hst.plugins.frontend.util.JcrUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * DAO of the SitemenuItem
 */
public class SitemenuItemDAO extends EditorDAO<SitemenuItem> {
    
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** The logger. */
    static final Logger log = LoggerFactory.getLogger(SitemenuItemDAO.class);

    /** SITEMAP_PROPERTY represents the jcr propertyname of the referenced sitemap item. */
    private static final String SITEMAP_PROPERTY = "hst:referencesitemapitem";

    /** EXTERNAL_URL_PROPERTY represents the jcr propertyname of the external url. */
    private static final String EXTERNAL_URL_PROPERTY = "hst:externallink";

    /**
     * Instantiates a new sitemenu item dao.
     * 
     * @param context the context
     * @param config the config
     */
    public SitemenuItemDAO(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO#load(org.hippoecm.frontend.model.JcrNodeModel)
     */
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
        if (JcrUtilities.hasProperty(model, EXTERNAL_URL_PROPERTY)) {
            item.setExternalLink((JcrUtilities.getProperty(model, EXTERNAL_URL_PROPERTY)));
        }

        return item;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO#persist(org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean, org.hippoecm.frontend.model.JcrNodeModel)
     */
    @Override
    protected void persist(SitemenuItem k, JcrNodeModel model) {
        HstContext ctx = getHstContext();

        //Set matcher value as nodeName if it's not already the same
        String newName = k.getName();
        try {
            if(!k.getName().equals(model.getNode().getName())){
                k.setModel(JcrUtilities.rename(model, newName));    
            }            
        } catch (RepositoryException e) {
            log.warn("Exception occured while trying to get name from model: ",e.getMessage());
        }

        //save sitemapReference
        JcrUtilities.updateProperty(model, SITEMAP_PROPERTY, k.getSitemapReference());
        //save externalLink
        JcrUtilities.updateProperty(model, EXTERNAL_URL_PROPERTY, k.getExternalLink());
    }

}
