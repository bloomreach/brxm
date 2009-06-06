package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.hippoecm.frontend.model.JcrNodeModel;

public class SitemenuItem extends EditorBean {

	private static final long serialVersionUID = 1L;

	public SitemenuItem(JcrNodeModel model) {
		super(model);
	}
	
	String name;
	String sitemapReference;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSitemapReference() {
		return sitemapReference;
	}

	public void setSitemapReference(String sitemapReference) {
		this.sitemapReference = sitemapReference;
	}


}
