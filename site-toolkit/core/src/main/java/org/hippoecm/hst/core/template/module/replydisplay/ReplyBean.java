package org.hippoecm.hst.core.template.module.replydisplay;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class ReplyBean {
	String name;
	String title;
	String content;
	
	ReplyBean(Node node) throws RepositoryException {
		System.out.println("CREATE NODE" + name + " " + title + " " + content);
		name = node.getProperty("name").getString();
		title = node.getProperty("title").getString();
		content = node.getProperty("content").getString();
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}
}
