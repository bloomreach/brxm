package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.hippoecm.frontend.model.JcrNodeModel;

public class Sitemenu extends EditorBean {

	private static final long serialVersionUID = 1L;

	public Sitemenu(JcrNodeModel model) {
		super(model);
	}
	
	String name;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
