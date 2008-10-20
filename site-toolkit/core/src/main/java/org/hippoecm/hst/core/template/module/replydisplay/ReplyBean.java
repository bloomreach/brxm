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
package org.hippoecm.hst.core.template.module.replydisplay;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplyBean {
    private static final Logger log = LoggerFactory.getLogger(ReplyBean.class);
	String name;
	String title;
	String content;
	
	ReplyBean(Node node) throws RepositoryException {
	    log.debug("CREATE NODE" + name + " " + title + " " + content);
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
