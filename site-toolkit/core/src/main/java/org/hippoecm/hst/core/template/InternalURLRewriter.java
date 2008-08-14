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
package org.hippoecm.hst.core.template;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalURLRewriter {
	private static final Logger logger = LoggerFactory.getLogger(InternalURLRewriter.class);
	private static final Pattern URL_PATTERN = Pattern.compile("((?:<a\\s.*?href=\"))([^:]*?)(\".*?>)");
	
	private Session jcrSession;
	
	public InternalURLRewriter(Session session) {
		jcrSession = session;
	}
	
	public StringBuffer rewrite(StringBuffer content) throws RepositoryException {
		    StringBuffer sb = null;
	        Matcher m = URL_PATTERN.matcher(content);
	        m.reset();
	        while (m.find()) {
	            if (sb == null) {
	                sb = new StringBuffer(content.length());
	            }
	            String documentPath = m.group(2);
	            String url = translateDocumentPathToURL(documentPath);
	            m.appendReplacement(sb, m.group(1) + url + m.group(3));
	        }

	        if (sb == null) {
	            return content;
	        } else {
	            m.appendTail(sb);
	            return sb;
	        }
	}
	
	private String translateDocumentPathToURL(String documentPath) throws RepositoryException {
		//get node (= internal url)
	    Item nodeItem = jcrSession.getItem(documentPath);
	    Node node = null;
	    if (nodeItem.isNode()) {
	       node = (Node) nodeItem;
	    }
	    node.getNodes();
		//get real node 
		//find mapping for read node
		//return mapping
	    return null;
	}
}
