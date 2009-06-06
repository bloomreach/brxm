package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.hippoecm.frontend.model.JcrNodeModel;

/**
 * The SitemenuItem class represents a domain object of the hst:sitemenuitem node.
 */
public class SitemenuItem extends EditorBean {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

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
