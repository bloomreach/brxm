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

package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.hippoecm.frontend.model.JcrNodeModel;

/**
 * The SitemenuItem class represents a domain object of the hst:sitemenuitem node.
 */
public class SitemenuItem extends EditorBean {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Instantiates a new sitemenu item.
     * 
     * @param model the jcr node model
     */
    public SitemenuItem(JcrNodeModel model) {
        super(model);
    }

    /** The name. */
    String name;

    /** The sitemap reference. */
    String sitemapReference;

    /** The external link. */
    String externalLink;

    /**
     * Gets the name of the sitemenu item.
     * 
     * @return the name of the sitemenu item
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the sitemenu item.
     * 
     * @param name the name of the sitemenu item
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the sitemap reference.
     * 
     * @return the sitemap reference
     */
    public String getSitemapReference() {
        return sitemapReference;
    }

    /**
     * Sets the sitemap reference.
     * 
     * @param sitemapReference the new sitemap reference
     */
    public void setSitemapReference(String sitemapReference) {
        this.sitemapReference = sitemapReference;
    }

    /**
     * Gets the external link.
     * 
     * @return the external link
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * Sets the external link.
     * 
     * @param externalLink the external link
     */
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

}
